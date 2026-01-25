package com.report.repository;

import com.report.config.DataSourceConfig;
import com.report.model.TaskProgress;
import com.report.model.TaskProgress.TaskStatus;
import com.report.model.TaskProgress.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for task progress operations
 */
public class TaskProgressRepository {
    private static final Logger logger = LoggerFactory.getLogger(TaskProgressRepository.class);
    private final DataSourceConfig dataSource;

    private static final String INSERT_SQL =
            "INSERT INTO report_task_progress (task_id, table_name, task_type, status, total_count, " +
            "processed_count, success_count, fail_count, last_processed_id, start_time) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE report_task_progress SET status = ?, processed_count = ?, success_count = ?, " +
            "fail_count = ?, last_processed_id = ?, end_time = ?, updated_at = NOW() WHERE id = ?";

    private static final String FIND_RUNNING_SQL =
            "SELECT * FROM report_task_progress WHERE table_name = ? AND status = 'RUNNING' " +
            "ORDER BY created_at DESC LIMIT 1";

    private static final String FIND_BY_TASK_ID_SQL =
            "SELECT * FROM report_task_progress WHERE task_id = ?";

    public TaskProgressRepository() {
        this.dataSource = DataSourceConfig.getInstance();
    }

    /**
     * Create new task progress record
     */
    public TaskProgress create(TaskProgress progress) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, progress.getTaskId());
            stmt.setString(2, progress.getTableName());
            stmt.setString(3, progress.getTaskType().name());
            stmt.setString(4, progress.getStatus().name());
            stmt.setLong(5, progress.getTotalCount());
            stmt.setLong(6, progress.getProcessedCount());
            stmt.setLong(7, progress.getSuccessCount());
            stmt.setLong(8, progress.getFailCount());
            stmt.setLong(9, progress.getLastProcessedId());
            stmt.setTimestamp(10, new Timestamp(progress.getStartTime().getTime()));

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    progress.setId(rs.getLong(1));
                }
            }

            logger.info("Created task progress: {}", progress.getTaskId());
            return progress;

        } catch (SQLException e) {
            logger.error("Failed to create task progress: {}", e.getMessage(), e);
            throw new RuntimeException("Database insert failed", e);
        }
    }

    /**
     * Update task progress
     */
    public void update(TaskProgress progress) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, progress.getStatus().name());
            stmt.setLong(2, progress.getProcessedCount());
            stmt.setLong(3, progress.getSuccessCount());
            stmt.setLong(4, progress.getFailCount());
            stmt.setLong(5, progress.getLastProcessedId());

            if (progress.getEndTime() != null) {
                stmt.setTimestamp(6, new Timestamp(progress.getEndTime().getTime()));
            } else {
                stmt.setNull(6, Types.TIMESTAMP);
            }

            stmt.setLong(7, progress.getId());

            stmt.executeUpdate();
            logger.debug("Updated task progress: {}", progress.getTaskId());

        } catch (SQLException e) {
            logger.error("Failed to update task progress: {}", e.getMessage(), e);
            throw new RuntimeException("Database update failed", e);
        }
    }

    /**
     * Find running task for specified table
     */
    public TaskProgress findRunningTask(String tableName) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_RUNNING_SQL)) {

            stmt.setString(1, tableName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to find running task for table {}: {}", tableName, e.getMessage(), e);
        }

        return null;
    }

    /**
     * Find task by task ID
     */
    public TaskProgress findByTaskId(String taskId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_TASK_ID_SQL)) {

            stmt.setString(1, taskId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to find task by ID {}: {}", taskId, e.getMessage(), e);
        }

        return null;
    }

    /**
     * Find all running tasks
     */
    public List<TaskProgress> findAllRunningTasks() {
        String sql = "SELECT * FROM report_task_progress WHERE status = 'RUNNING'";
        List<TaskProgress> tasks = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                tasks.add(mapResultSet(rs));
            }

        } catch (SQLException e) {
            logger.error("Failed to find running tasks: {}", e.getMessage(), e);
        }

        return tasks;
    }

    /**
     * Mark task as completed
     */
    public void complete(TaskProgress progress) {
        progress.complete();
        update(progress);
        logger.info("Task completed: {} - success: {}, fail: {}",
                progress.getTaskId(), progress.getSuccessCount(), progress.getFailCount());
    }

    /**
     * Mark task as failed
     */
    public void fail(TaskProgress progress) {
        progress.fail();
        update(progress);
        logger.error("Task failed: {}", progress.getTaskId());
    }

    private TaskProgress mapResultSet(ResultSet rs) throws SQLException {
        TaskProgress progress = new TaskProgress();
        progress.setId(rs.getLong("id"));
        progress.setTaskId(rs.getString("task_id"));
        progress.setTableName(rs.getString("table_name"));
        progress.setTaskType(TaskType.valueOf(rs.getString("task_type")));
        progress.setStatus(TaskStatus.valueOf(rs.getString("status")));
        progress.setTotalCount(rs.getLong("total_count"));
        progress.setProcessedCount(rs.getLong("processed_count"));
        progress.setSuccessCount(rs.getLong("success_count"));
        progress.setFailCount(rs.getLong("fail_count"));
        progress.setLastProcessedId(rs.getLong("last_processed_id"));

        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            progress.setStartTime(new java.util.Date(startTime.getTime()));
        }

        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            progress.setEndTime(new java.util.Date(endTime.getTime()));
        }

        return progress;
    }

    /**
     * Initialize the task progress table if not exists
     */
    public void initTable() {
        String sql = "CREATE TABLE IF NOT EXISTS report_task_progress (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "task_id VARCHAR(50) NOT NULL, " +
                "table_name VARCHAR(50) NOT NULL, " +
                "task_type VARCHAR(20) NOT NULL, " +
                "status VARCHAR(20) NOT NULL, " +
                "total_count BIGINT DEFAULT 0, " +
                "processed_count BIGINT DEFAULT 0, " +
                "success_count BIGINT DEFAULT 0, " +
                "fail_count BIGINT DEFAULT 0, " +
                "last_processed_id BIGINT DEFAULT 0, " +
                "start_time DATETIME, " +
                "end_time DATETIME, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "INDEX idx_task_id (task_id), " +
                "INDEX idx_status (status), " +
                "INDEX idx_table_name (table_name)" +
                ")";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("Task progress table initialized");
        } catch (SQLException e) {
            logger.error("Failed to initialize task progress table: {}", e.getMessage(), e);
        }
    }
}
