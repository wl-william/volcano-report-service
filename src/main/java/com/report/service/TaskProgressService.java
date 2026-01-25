package com.report.service;

import com.report.model.TaskProgress;
import com.report.model.TaskProgress.TaskStatus;
import com.report.model.TaskProgress.TaskType;
import com.report.repository.TaskProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing task progress (checkpoint/resume support)
 */
public class TaskProgressService {
    private static final Logger logger = LoggerFactory.getLogger(TaskProgressService.class);

    private final TaskProgressRepository repository;

    public TaskProgressService() {
        this.repository = new TaskProgressRepository();
    }

    /**
     * Find existing running task or create new one
     */
    public TaskProgress findOrCreateTask(String tableName, TaskType taskType) {
        // Try to find existing running task
        TaskProgress existing = repository.findRunningTask(tableName);

        if (existing != null) {
            logger.info("Resuming existing task {} for table {} (lastId={})",
                    existing.getTaskId(), tableName, existing.getLastProcessedId());
            return existing;
        }

        // Create new task
        String taskId = generateTaskId(tableName);
        TaskProgress newTask = new TaskProgress(taskId, tableName, taskType);
        repository.create(newTask);

        logger.info("Created new task {} for table {}", taskId, tableName);
        return newTask;
    }

    /**
     * Create a new task
     */
    public TaskProgress createTask(String tableName, TaskType taskType, long totalCount) {
        String taskId = generateTaskId(tableName);
        TaskProgress task = new TaskProgress(taskId, tableName, taskType);
        task.setTotalCount(totalCount);
        return repository.create(task);
    }

    /**
     * Update task progress
     */
    public void updateProgress(TaskProgress progress) {
        repository.update(progress);
    }

    /**
     * Mark task as completed
     */
    public void completeTask(TaskProgress progress) {
        repository.complete(progress);
    }

    /**
     * Mark task as failed
     */
    public void failTask(TaskProgress progress) {
        repository.fail(progress);
    }

    /**
     * Pause a running task
     */
    public void pauseTask(TaskProgress progress) {
        progress.pause();
        repository.update(progress);
        logger.info("Task {} paused at id={}", progress.getTaskId(), progress.getLastProcessedId());
    }

    /**
     * Resume a paused task
     */
    public void resumeTask(TaskProgress progress) {
        progress.resume();
        repository.update(progress);
        logger.info("Task {} resumed from id={}", progress.getTaskId(), progress.getLastProcessedId());
    }

    /**
     * Find task by ID
     */
    public TaskProgress findByTaskId(String taskId) {
        return repository.findByTaskId(taskId);
    }

    /**
     * Find running task for table
     */
    public TaskProgress findRunningTask(String tableName) {
        return repository.findRunningTask(tableName);
    }

    /**
     * Get all running tasks
     */
    public List<TaskProgress> getAllRunningTasks() {
        return repository.findAllRunningTasks();
    }

    /**
     * Check if there are any running tasks
     */
    public boolean hasRunningTasks() {
        List<TaskProgress> tasks = repository.findAllRunningTasks();
        return !tasks.isEmpty();
    }

    /**
     * Initialize task progress table
     */
    public void initTable() {
        repository.initTable();
    }

    /**
     * Generate unique task ID
     */
    private String generateTaskId(String tableName) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return tableName + "_" + System.currentTimeMillis() + "_" + uuid;
    }

    /**
     * Print task summary
     */
    public void printTaskSummary(TaskProgress progress) {
        logger.info("Task Summary: {}", progress.getTaskId());
        logger.info("  Table: {}", progress.getTableName());
        logger.info("  Status: {}", progress.getStatus());
        logger.info("  Progress: {}/{}", progress.getProcessedCount(), progress.getTotalCount());
        logger.info("  Success: {}", progress.getSuccessCount());
        logger.info("  Failed: {}", progress.getFailCount());
        logger.info("  Last Processed ID: {}", progress.getLastProcessedId());
    }
}
