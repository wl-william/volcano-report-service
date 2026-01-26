package com.report.repository;

import com.report.config.DataSourceConfig;
import com.report.config.EventTableConfig;
import com.report.model.ReportStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Repository for event data operations across multiple tables
 */
public class EventDataRepository {
    private static final Logger logger = LoggerFactory.getLogger(EventDataRepository.class);
    private final DataSourceConfig dataSource;

    public EventDataRepository() {
        this.dataSource = DataSourceConfig.getInstance();
    }

    /**
     * Fetch pending records from specified table
     *
     * @param tableName       Table name (event type)
     * @param lastProcessedId Last processed ID for checkpoint
     * @param limit           Batch size
     * @return List of records as Map
     */
    public List<Map<String, Object>> fetchPendingRecords(String tableName, long lastProcessedId, int limit) {
        EventTableConfig tableConfig = EventTableConfig.getByTableName(tableName);
        if (tableConfig == null) {
            logger.error("Unknown table name: {}", tableName);
            return Collections.emptyList();
        }

        String sql = tableConfig.buildPendingQuery();
        List<Map<String, Object>> records = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, lastProcessedId);
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> record = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        record.put(columnName, value);
                    }
                    records.add(record);
                }
            }

            logger.debug("Fetched {} records from table {} (lastId={})", records.size(), tableName, lastProcessedId);

        } catch (SQLException e) {
            logger.error("Failed to fetch records from table {}: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("Database query failed", e);
        }

        return records;
    }

    /**
     * Get pending record count for specified table
     */
    public long getPendingCount(String tableName) {
        EventTableConfig tableConfig = EventTableConfig.getByTableName(tableName);
        if (tableConfig == null) {
            logger.error("Unknown table name: {}", tableName);
            return 0;
        }

        String sql = tableConfig.buildPendingCountQuery();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }

        } catch (SQLException e) {
            logger.error("Failed to get pending count for table {}: {}", tableName, e.getMessage(), e);
        }

        return 0;
    }

    /**
     * Get pending counts for all event tables
     */
    public Map<String, Long> getAllPendingCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (EventTableConfig config : EventTableConfig.values()) {
            long count = getPendingCount(config.getTableName());
            counts.put(config.getTableName(), count);
        }
        return counts;
    }

    /**
     * Update report status for records
     *
     * @param tableName Table name
     * @param ids       Record IDs
     * @param status    New status (0:pending, 1:processing, 2:success, 3:failed)
     */
    public void updateStatus(String tableName, List<Long> ids, int status) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        EventTableConfig tableConfig = EventTableConfig.getByTableName(tableName);
        if (tableConfig == null) {
            logger.error("Unknown table name: {}", tableName);
            return;
        }

        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = String.format(
                "UPDATE %s SET report_status = ?, updated_at = NOW() WHERE id IN (%s)",
                tableName, placeholders
        );

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, status);
            int paramIndex = 2;
            for (Long id : ids) {
                stmt.setLong(paramIndex++, id);
            }

            int updated = stmt.executeUpdate();
            logger.debug("Updated {} records in table {} to status {}", updated, tableName, status);

        } catch (SQLException e) {
            logger.error("Failed to update status for table {}: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("Database update failed", e);
        }
    }

    /**
     * Update status with error message for failed records
     */
    public void updateStatusWithError(String tableName, Long id, int status, String errorMsg) {
        EventTableConfig tableConfig = EventTableConfig.getByTableName(tableName);
        if (tableConfig == null) {
            return;
        }

        String sql = String.format(
                "UPDATE %s SET report_status = ?, error_msg = ?, retry_count = retry_count + 1, updated_at = NOW() WHERE id = ?",
                tableName
        );

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, status);
            stmt.setString(2, truncate(errorMsg, 500));
            stmt.setLong(3, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            logger.error("Failed to update error status for table {} id {}: {}", tableName, id, e.getMessage());
        }
    }

    /**
     * Mark records as processing
     */
    public void markAsProcessing(String tableName, List<Long> ids) {
        updateStatus(tableName, ids, ReportStatus.PROCESSING.getCode());
    }

    /**
     * Mark records as success
     */
    public void markAsSuccess(String tableName, List<Long> ids) {
        updateStatus(tableName, ids, ReportStatus.SUCCESS.getCode());
    }

    /**
     * Mark records as failed
     */
    public void markAsFailed(String tableName, List<Long> ids) {
        updateStatus(tableName, ids, ReportStatus.FAILED.getCode());
    }

    /**
     * Get records that need retry (status = FAILED and retry_count < max)
     */
    public List<Map<String, Object>> fetchFailedRecords(String tableName, int maxRetryCount, int limit) {
        EventTableConfig tableConfig = EventTableConfig.getByTableName(tableName);
        if (tableConfig == null) {
            return Collections.emptyList();
        }

        String sql = String.format(
                "SELECT %s FROM %s WHERE report_status = %d AND retry_count < ? ORDER BY id LIMIT ?",
                tableConfig.buildSelectFields(),
                tableName,
                ReportStatus.FAILED.getCode()
        );

        List<Map<String, Object>> records = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, maxRetryCount);
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> record = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        record.put(metaData.getColumnName(i), rs.getObject(i));
                    }
                    records.add(record);
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to fetch failed records from table {}: {}", tableName, e.getMessage(), e);
        }

        return records;
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }
}
