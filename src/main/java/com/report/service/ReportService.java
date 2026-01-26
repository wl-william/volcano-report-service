package com.report.service;

import com.report.config.AppConfig;
import com.report.config.EventTableConfig;
import com.report.config.ReportMode;
import com.report.model.ReportPayload;
import com.report.model.ReportResult;
import com.report.repository.EventDataRepository;
import com.report.util.HttpClientUtil;
import com.report.util.JsonUtil;
import com.report.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Main service for reporting events to Volcano Engine
 * Stateless date-based processing for Hive partitioned tables
 */
public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private static final Logger failedLogger = LoggerFactory.getLogger("FAILED_RECORDS");

    private static final String SINGLE_ENDPOINT = "/v2/event/json";
    private static final String BATCH_ENDPOINT = "/v2/event/list";
    private static final int BATCH_SIZE = 1000;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final AppConfig config;
    private final HttpClientUtil httpClient;
    private final EventDataRepository dataRepository;
    private final DataTransformService transformService;

    public ReportService() {
        this.config = AppConfig.getInstance();
        this.httpClient = HttpClientUtil.getInstance();
        this.dataRepository = new EventDataRepository();
        this.transformService = new DataTransformService();
    }

    /**
     * Process all tables for a specific date
     *
     * @param dt Date partition (e.g., "2026-01-26")
     */
    public void processDate(String dt) {
        logger.info("========== Starting date-based report task ==========");
        logger.info("Processing date: {}", dt);

        int totalSuccess = 0;
        int totalFail = 0;
        int totalRecords = 0;

        for (String tableName : config.getEventTables()) {
            logger.info("Processing table: {}", tableName);

            try {
                TableResult result = processTable(tableName, dt);
                totalRecords += result.totalRecords;
                totalSuccess += result.successCount;
                totalFail += result.failCount;

                logger.info("Table {} completed: total={}, success={}, fail={}",
                        tableName, result.totalRecords, result.successCount, result.failCount);

            } catch (Exception e) {
                logger.error("Failed to process table {}: {}", tableName, e.getMessage(), e);
                totalFail++;
            }
        }

        logger.info("========== Date-based report completed ==========");
        logger.info("Summary: total={}, success={}, fail={}", totalRecords, totalSuccess, totalFail);
    }

    /**
     * Process a single table for a specific date
     *
     * @param tableName Table name
     * @param dt        Date partition
     * @return Processing result
     */
    private TableResult processTable(String tableName, String dt) {
        long totalCount = dataRepository.count(tableName, dt);
        logger.info("Total records in {} (dt={}): {}", tableName, dt, totalCount);

        if (totalCount == 0) {
            return new TableResult(0, 0, 0);
        }

        // Get report mode for this table
        EventTableConfig tableConfig = EventTableConfig.getByTableName(tableName);
        ReportMode reportMode = tableConfig != null ? tableConfig.getReportMode() : ReportMode.BATCH;
        logger.info("Table {} using report mode: {}", tableName, reportMode);

        int successCount = 0;
        int failCount = 0;
        int offset = 0;

        while (offset < totalCount) {
            // Fetch batch with pagination
            List<Map<String, Object>> records = dataRepository.queryWithOffset(
                    tableName, dt, BATCH_SIZE, offset);

            if (records.isEmpty()) {
                break;
            }

            logger.info("Processing batch: table={}, dt={}, offset={}, size={}, mode={}",
                    tableName, dt, offset, records.size(), reportMode);

            // Process records based on report mode
            if (reportMode == ReportMode.SINGLE) {
                // Single mode: report one by one
                for (Map<String, Object> record : records) {
                    boolean success = processRecordWithRetry(tableName, dt, record);
                    if (success) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                }
            } else {
                // Batch mode: report in batches
                BatchResult batchResult = processBatchRecords(tableName, dt, records);
                successCount += batchResult.successCount;
                failCount += batchResult.failCount;
            }

            offset += records.size();
            logger.info("Batch completed: offset={}, success={}, fail={}", offset, successCount, failCount);
        }

        return new TableResult((int) totalCount, successCount, failCount);
    }

    /**
     * Process records in batch mode
     * Splits records into smaller batches (up to 20) and reports with retry
     *
     * @param tableName Table name
     * @param dt        Date partition
     * @param records   List of records to process
     * @return BatchResult with success and fail counts
     */
    private BatchResult processBatchRecords(String tableName, String dt, List<Map<String, Object>> records) {
        int successCount = 0;
        int failCount = 0;
        int reportBatchSize = config.getReportBatchSize(); // typically 20

        // Transform all records first
        List<ReportPayload> allPayloads = new ArrayList<>();
        for (Map<String, Object> record : records) {
            try {
                ReportPayload payload = transformService.transform(tableName, record);
                allPayloads.add(payload);
            } catch (Exception e) {
                logger.error("Failed to transform record from table {}: {}", tableName, e.getMessage());
                logFailedRecord(tableName, dt, record, "Transform failed: " + e.getMessage());
                failCount++;
            }
        }

        // Split into smaller batches and report
        for (int i = 0; i < allPayloads.size(); i += reportBatchSize) {
            int end = Math.min(i + reportBatchSize, allPayloads.size());
            List<ReportPayload> batch = allPayloads.subList(i, end);

            // Retry batch report
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    ReportResult result = reportBatch(batch);

                    if (result.isSuccess()) {
                        successCount += batch.size();
                        if (attempt > 1) {
                            logger.info("Batch reported successfully on attempt {}: table={}, size={}",
                                    attempt, tableName, batch.size());
                        }
                        break; // success, move to next batch
                    }

                    logger.warn("Batch report attempt {} failed for table {}: {}",
                            attempt, tableName, result.getErrorMessage());

                    // Last attempt failed
                    if (attempt == MAX_RETRIES) {
                        failCount += batch.size();
                        // Log all records in the failed batch
                        for (ReportPayload payload : batch) {
                            Map<String, Object> originalRecord = records.get(allPayloads.indexOf(payload));
                            logFailedRecord(tableName, dt, originalRecord,
                                    "Batch report failed: " + result.getErrorMessage());
                        }
                    } else {
                        // Wait before retry
                        Thread.sleep(RETRY_DELAY_MS);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Batch report retry interrupted for table {}", tableName);
                    failCount += batch.size();
                    break;
                } catch (Exception e) {
                    logger.error("Unexpected error on batch report attempt {} for table {}: {}",
                            attempt, tableName, e.getMessage());

                    if (attempt == MAX_RETRIES) {
                        failCount += batch.size();
                    } else {
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            failCount += batch.size();
                            break;
                        }
                    }
                }
            }
        }

        return new BatchResult(successCount, failCount);
    }

    /**
     * Process a single record with retry logic
     *
     * @param tableName Table name
     * @param dt        Date partition
     * @param record    Record data
     * @return true if successful, false otherwise
     */
    private boolean processRecordWithRetry(String tableName, String dt, Map<String, Object> record) {
        // Transform to payload
        ReportPayload payload;
        try {
            payload = transformService.transform(tableName, record);
        } catch (Exception e) {
            logger.error("Failed to transform record from table {}: {}", tableName, e.getMessage());
            logFailedRecord(tableName, dt, record, "Transform failed: " + e.getMessage());
            return false;
        }

        // Retry up to MAX_RETRIES times
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                ReportResult result = reportSingle(payload);

                if (result.isSuccess()) {
                    if (attempt > 1) {
                        logger.info("Record reported successfully on attempt {}: table={}, user={}",
                                attempt, tableName,
                                LogSanitizer.sanitizeUserId(payload.getUser().getUserUniqueId()));
                    }
                    return true;
                }

                logger.warn("Report attempt {} failed for table {}: {}",
                        attempt, tableName, result.getErrorMessage());

                // Wait before retry
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Retry interrupted for table {}", tableName);
                logFailedRecord(tableName, dt, record, "Retry interrupted");
                return false;
            } catch (Exception e) {
                logger.error("Unexpected error on attempt {} for table {}: {}",
                        attempt, tableName, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logFailedRecord(tableName, dt, record, "Retry interrupted: " + e.getMessage());
                        return false;
                    }
                }
            }
        }

        // All retries failed
        logFailedRecord(tableName, dt, record, "Max retries exceeded");
        return false;
    }

    /**
     * Log failed record for manual review
     */
    private void logFailedRecord(String tableName, String dt, Map<String, Object> record, String reason) {
        String userUniqueId = record.get("user_unique_id") != null
                ? record.get("user_unique_id").toString()
                : "unknown";

        failedLogger.error("FAILED: table={}, dt={}, user={}, reason={}, record={}",
                tableName, dt,
                LogSanitizer.sanitizeUserId(userUniqueId),
                reason,
                JsonUtil.toJson(record));
    }

    /**
     * Report single payload
     */
    public ReportResult reportSingle(ReportPayload payload) {
        String json = JsonUtil.toJson(payload);
        return httpClient.post(SINGLE_ENDPOINT, json);
    }

    /**
     * Report batch of payloads (max 50, recommended 20)
     */
    public ReportResult reportBatch(List<ReportPayload> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            return ReportResult.success(0);
        }

        if (payloads.size() > 50) {
            logger.warn("Batch size {} exceeds maximum 50, truncating", payloads.size());
            payloads = payloads.subList(0, 50);
        }

        String json = JsonUtil.toJson(payloads);
        return httpClient.post(BATCH_ENDPOINT, json);
    }

    /**
     * Show statistics for a specific date
     *
     * @param dt Date partition (e.g., "2026-01-26")
     */
    public void showStats(String dt) {
        logger.info("========== Statistics for date: {} ==========", dt);

        long totalRecords = 0;

        System.out.println("\n========== Statistics for " + dt + " ==========");

        for (String tableName : config.getEventTables()) {
            long count = dataRepository.count(tableName, dt);
            totalRecords += count;

            System.out.printf("  %-20s : %d%n", tableName, count);
            logger.info("Table {}: {} records", tableName, count);
        }

        System.out.println("------------------------------------------------");
        System.out.printf("  %-20s : %d%n", "TOTAL", totalRecords);
        System.out.println("================================================\n");

        logger.info("Total records for {}: {}", dt, totalRecords);
    }

    /**
     * Get yesterday's date in format YYYY-MM-DD
     */
    public static String getYesterdayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        long yesterday = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
        return sdf.format(new Date(yesterday));
    }

    /**
     * Result of processing a single table
     */
    private static class TableResult {
        final int totalRecords;
        final int successCount;
        final int failCount;

        TableResult(int totalRecords, int successCount, int failCount) {
            this.totalRecords = totalRecords;
            this.successCount = successCount;
            this.failCount = failCount;
        }
    }

    /**
     * Result of batch processing
     */
    private static class BatchResult {
        final int successCount;
        final int failCount;

        BatchResult(int successCount, int failCount) {
            this.successCount = successCount;
            this.failCount = failCount;
        }
    }
}
