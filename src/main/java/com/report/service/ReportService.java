package com.report.service;

import com.report.config.AppConfig;
import com.report.model.ReportPayload;
import com.report.model.ReportResult;
import com.report.model.TaskProgress;
import com.report.model.TaskProgress.TaskType;
import com.report.util.HttpClientUtil;
import com.report.util.JsonUtil;
import com.report.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Main service for reporting events to Volcano Engine
 */
public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private static final Logger failedLogger = LoggerFactory.getLogger("FAILED_RECORDS");

    private static final String SINGLE_ENDPOINT = "/v2/event/json";
    private static final String BATCH_ENDPOINT = "/v2/event/list";

    private final AppConfig config;
    private final HttpClientUtil httpClient;
    private final DataFetchService dataFetchService;
    private final DataTransformService transformService;
    private final TaskProgressService progressService;

    public ReportService() {
        this.config = AppConfig.getInstance();
        this.httpClient = HttpClientUtil.getInstance();
        this.dataFetchService = new DataFetchService();
        this.transformService = new DataTransformService();
        this.progressService = new TaskProgressService();
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
     * Execute full report for all tables
     */
    public void executeFullReport() {
        logger.info("========== Starting full report task ==========");

        Map<String, Long> pendingCounts = dataFetchService.getAllPendingCounts();
        long totalPending = pendingCounts.values().stream().mapToLong(Long::longValue).sum();

        if (totalPending == 0) {
            logger.info("No pending records to report");
            return;
        }

        logger.info("Total pending records: {}", totalPending);

        // Process each table
        for (String tableName : config.getEventTables()) {
            Long count = pendingCounts.get(tableName);
            if (count != null && count > 0) {
                processTable(tableName);
            }
        }

        logger.info("========== Full report task completed ==========");
    }

    /**
     * Process a single table with checkpoint support
     */
    public void processTable(String tableName) {
        logger.info("Processing table: {}", tableName);

        // Check for existing running task (resume)
        TaskProgress progress = progressService.findOrCreateTask(tableName, TaskType.INCR);
        long lastProcessedId = progress.getLastProcessedId();

        logger.info("Task {} started for table {} (resuming from id={})",
                progress.getTaskId(), tableName, lastProcessedId);

        try {
            while (true) {
                // Fetch batch
                List<Map<String, Object>> records = dataFetchService.fetchBatch(tableName, lastProcessedId);

                if (records.isEmpty()) {
                    logger.info("No more records for table {}", tableName);
                    break;
                }

                logger.info("Fetched {} records from table {} (after id={})",
                        records.size(), tableName, lastProcessedId);

                // Transform to payloads
                List<ReportPayload> payloads = transformService.transformBatch(tableName, records);
                List<Long> recordIds = transformService.extractRecordIds(payloads);

                // Mark as processing
                dataFetchService.markAsProcessing(tableName, recordIds);

                // Report in batches
                int batchSize = config.getReportBatchSize();
                int successCount = 0;
                int failCount = 0;

                for (int i = 0; i < payloads.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, payloads.size());
                    List<ReportPayload> batch = payloads.subList(i, end);
                    List<Long> batchIds = transformService.extractRecordIds(batch);

                    ReportResult result = reportBatchWithRetry(batch);

                    if (result.isSuccess()) {
                        dataFetchService.markAsSuccess(tableName, batchIds);
                        successCount += batchIds.size();
                    } else {
                        handleFailedBatch(tableName, batch, result.getErrorMessage());
                        failCount += batchIds.size();
                    }
                }

                // Update progress
                lastProcessedId = transformService.getMaxRecordId(payloads);
                progress.setLastProcessedId(lastProcessedId);
                progress.incrementProcessed(records.size());
                progress.incrementSuccess(successCount);
                progress.incrementFail(failCount);
                progressService.updateProgress(progress);

                logger.info("Batch completed: success={}, fail={}, lastId={}",
                        successCount, failCount, lastProcessedId);
            }

            // Complete task
            progressService.completeTask(progress);
            logger.info("Table {} processing completed: success={}, fail={}",
                    tableName, progress.getSuccessCount(), progress.getFailCount());

        } catch (Exception e) {
            logger.error("Error processing table {}: {}", tableName, e.getMessage(), e);
            progressService.failTask(progress);
            throw new RuntimeException("Table processing failed: " + tableName, e);
        }
    }

    /**
     * Report batch with retry
     * Note: Uses synchronous retry with Thread.sleep for simplicity in batch processing context.
     * For high-concurrency scenarios, consider using ScheduledExecutorService or reactive patterns.
     */
    private ReportResult reportBatchWithRetry(List<ReportPayload> payloads) {
        int maxRetries = config.getMaxRetryTimes();
        long retryInterval = config.getRetryIntervalMs();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            ReportResult result = reportBatch(payloads);

            if (result.isSuccess()) {
                return result;
            }

            if (attempt < maxRetries) {
                logger.warn("Report attempt {} failed, retrying in {}ms: {}",
                        attempt, retryInterval, result.getErrorMessage());
                try {
                    // Synchronous sleep is acceptable here as this is batch processing
                    // and we want to avoid overwhelming the API with rapid retries
                    Thread.sleep(retryInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Retry interrupted, returning last result");
                    return result;
                }
            }
        }

        logger.error("All {} report attempts failed", maxRetries);
        return ReportResult.failure(0, "Max retries exceeded");
    }

    /**
     * Handle failed batch - log and mark as failed
     */
    private void handleFailedBatch(String tableName, List<ReportPayload> payloads, String errorMessage) {
        for (ReportPayload payload : payloads) {
            Long recordId = payload.getRecordId();
            dataFetchService.markAsFailed(tableName, recordId, errorMessage);

            // Log to failed records file (sanitize user ID for privacy)
            failedLogger.info("table={}, id={}, user={}, error={}",
                    tableName, recordId,
                    LogSanitizer.sanitizeUserId(payload.getUser().getUserUniqueId()),
                    errorMessage);
        }
    }

    /**
     * Retry failed records for all tables
     */
    public void retryFailedRecords() {
        logger.info("Starting retry of failed records");

        for (String tableName : config.getEventTables()) {
            retryFailedRecordsForTable(tableName);
        }

        logger.info("Retry of failed records completed");
    }

    /**
     * Retry failed records for a specific table
     */
    private void retryFailedRecordsForTable(String tableName) {
        List<Map<String, Object>> failedRecords =
                dataFetchService.fetchFailedRecords(tableName, config.getDbBatchSize());

        if (failedRecords.isEmpty()) {
            return;
        }

        logger.info("Retrying {} failed records for table {}", failedRecords.size(), tableName);

        List<ReportPayload> payloads = transformService.transformBatch(tableName, failedRecords);

        int batchSize = config.getReportBatchSize();
        for (int i = 0; i < payloads.size(); i += batchSize) {
            int end = Math.min(i + batchSize, payloads.size());
            List<ReportPayload> batch = payloads.subList(i, end);
            List<Long> batchIds = transformService.extractRecordIds(batch);

            ReportResult result = reportBatch(batch);

            if (result.isSuccess()) {
                dataFetchService.markAsSuccess(tableName, batchIds);
                logger.info("Retry success for {} records in table {}", batchIds.size(), tableName);
            } else {
                for (ReportPayload payload : batch) {
                    dataFetchService.markAsFailed(tableName, payload.getRecordId(), result.getErrorMessage());
                }
            }
        }
    }

    /**
     * Get report statistics
     */
    public Map<String, Long> getStatistics() {
        return dataFetchService.getAllPendingCounts();
    }
}
