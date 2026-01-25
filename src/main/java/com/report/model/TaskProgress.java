package com.report.model;

import java.util.Date;

/**
 * Task progress model for checkpoint/resume support
 */
public class TaskProgress {

    private Long id;
    private String taskId;
    private String tableName;
    private TaskType taskType;
    private TaskStatus status;
    private long totalCount;
    private long processedCount;
    private long successCount;
    private long failCount;
    private long lastProcessedId;
    private Date startTime;
    private Date endTime;
    private Date createdAt;
    private Date updatedAt;

    public enum TaskType {
        FULL,   // Full sync
        INCR    // Incremental sync
    }

    public enum TaskStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        PAUSED
    }

    public TaskProgress() {
    }

    public TaskProgress(String taskId, String tableName, TaskType taskType) {
        this.taskId = taskId;
        this.tableName = tableName;
        this.taskType = taskType;
        this.status = TaskStatus.RUNNING;
        this.totalCount = 0;
        this.processedCount = 0;
        this.successCount = 0;
        this.failCount = 0;
        this.lastProcessedId = 0;
        this.startTime = new Date();
    }

    public void incrementProcessed(int count) {
        this.processedCount += count;
    }

    public void incrementSuccess(int count) {
        this.successCount += count;
    }

    public void incrementFail(int count) {
        this.failCount += count;
    }

    public void complete() {
        this.status = TaskStatus.COMPLETED;
        this.endTime = new Date();
    }

    public void fail() {
        this.status = TaskStatus.FAILED;
        this.endTime = new Date();
    }

    public void pause() {
        this.status = TaskStatus.PAUSED;
    }

    public void resume() {
        this.status = TaskStatus.RUNNING;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getProcessedCount() {
        return processedCount;
    }

    public void setProcessedCount(long processedCount) {
        this.processedCount = processedCount;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    public long getFailCount() {
        return failCount;
    }

    public void setFailCount(long failCount) {
        this.failCount = failCount;
    }

    public long getLastProcessedId() {
        return lastProcessedId;
    }

    public void setLastProcessedId(long lastProcessedId) {
        this.lastProcessedId = lastProcessedId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "TaskProgress{" +
                "taskId='" + taskId + '\'' +
                ", tableName='" + tableName + '\'' +
                ", status=" + status +
                ", processed=" + processedCount + "/" + totalCount +
                ", success=" + successCount +
                ", fail=" + failCount +
                ", lastId=" + lastProcessedId +
                '}';
    }
}
