package com.report.service;

import com.report.config.AppConfig;
import com.report.config.EventTableConfig;
import com.report.model.ReportStatus;
import com.report.repository.EventDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Service for fetching event data from multiple tables
 */
public class DataFetchService {
    private static final Logger logger = LoggerFactory.getLogger(DataFetchService.class);

    private final EventDataRepository eventDataRepository;
    private final AppConfig config;

    public DataFetchService() {
        this.eventDataRepository = new EventDataRepository();
        this.config = AppConfig.getInstance();
    }

    /**
     * Fetch a batch of pending records from specified table
     *
     * @param tableName       Table name (event type)
     * @param lastProcessedId Last processed ID for checkpoint
     * @return List of records
     */
    public List<Map<String, Object>> fetchBatch(String tableName, long lastProcessedId) {
        return fetchBatch(tableName, lastProcessedId, config.getDbBatchSize());
    }

    /**
     * Fetch a batch of pending records with custom limit
     */
    public List<Map<String, Object>> fetchBatch(String tableName, long lastProcessedId, int limit) {
        if (!EventTableConfig.isValidTable(tableName)) {
            logger.error("Invalid table name: {}", tableName);
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        logger.debug("Fetching batch from table {} (lastId={}, limit={})", tableName, lastProcessedId, limit);
        return eventDataRepository.fetchPendingRecords(tableName, lastProcessedId, limit);
    }

    /**
     * Get pending record count for specified table
     */
    public long getPendingCount(String tableName) {
        if (!EventTableConfig.isValidTable(tableName)) {
            return 0;
        }
        return eventDataRepository.getPendingCount(tableName);
    }

    /**
     * Get pending counts for all event tables
     */
    public Map<String, Long> getAllPendingCounts() {
        Map<String, Long> counts = eventDataRepository.getAllPendingCounts();
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        logger.info("Total pending records across all tables: {}", total);
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            if (entry.getValue() > 0) {
                logger.info("  {} : {} pending", entry.getKey(), entry.getValue());
            }
        }
        return counts;
    }

    /**
     * Get all configured table names
     */
    public String[] getTableNames() {
        return config.getEventTables();
    }

    /**
     * Fetch failed records that need retry
     */
    public List<Map<String, Object>> fetchFailedRecords(String tableName, int limit) {
        return eventDataRepository.fetchFailedRecords(tableName, config.getMaxRetryTimes(), limit);
    }

    /**
     * Mark records as processing
     */
    public void markAsProcessing(String tableName, List<Long> ids) {
        eventDataRepository.markAsProcessing(tableName, ids);
    }

    /**
     * Mark records as success
     */
    public void markAsSuccess(String tableName, List<Long> ids) {
        eventDataRepository.markAsSuccess(tableName, ids);
    }

    /**
     * Mark records as failed with error message
     */
    public void markAsFailed(String tableName, Long id, String errorMsg) {
        eventDataRepository.updateStatusWithError(tableName, id, ReportStatus.FAILED.getCode(), errorMsg);
    }

    /**
     * Mark multiple records as failed
     */
    public void markAsFailed(String tableName, List<Long> ids) {
        eventDataRepository.markAsFailed(tableName, ids);
    }
}
