package com.report.schedule;

import com.report.config.AppConfig;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quartz scheduler configuration
 */
public class ScheduleConfig {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleConfig.class);

    private Scheduler scheduler;
    private final AppConfig config;

    public ScheduleConfig() {
        this.config = AppConfig.getInstance();
    }

    /**
     * Initialize and start the scheduler
     */
    public void start() throws SchedulerException {
        if (!config.isScheduleEnabled()) {
            logger.info("Scheduler is disabled");
            return;
        }

        scheduler = StdSchedulerFactory.getDefaultScheduler();

        // Schedule increment report job
        scheduleIncrementJob();

        // Schedule retry job
        scheduleRetryJob();

        scheduler.start();
        logger.info("Scheduler started");
    }

    /**
     * Schedule the increment report job
     */
    private void scheduleIncrementJob() throws SchedulerException {
        JobDetail job = JobBuilder.newJob(IncrementReportJob.class)
                .withIdentity("incrementReportJob", "reportGroup")
                .withDescription("Incremental data report job")
                .build();

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("incrementTrigger", "reportGroup")
                .withSchedule(CronScheduleBuilder.cronSchedule(config.getIncrementCron())
                        .withMisfireHandlingInstructionDoNothing())
                .build();

        scheduler.scheduleJob(job, trigger);
        logger.info("Increment report job scheduled with cron: {}", config.getIncrementCron());
    }

    /**
     * Schedule the retry job for failed records
     */
    private void scheduleRetryJob() throws SchedulerException {
        JobDetail job = JobBuilder.newJob(RetryReportJob.class)
                .withIdentity("retryReportJob", "reportGroup")
                .withDescription("Retry failed records job")
                .build();

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("retryTrigger", "reportGroup")
                .withSchedule(CronScheduleBuilder.cronSchedule(config.getRetryCron())
                        .withMisfireHandlingInstructionDoNothing())
                .build();

        scheduler.scheduleJob(job, trigger);
        logger.info("Retry report job scheduled with cron: {}", config.getRetryCron());
    }

    /**
     * Shutdown the scheduler
     */
    public void shutdown() {
        if (scheduler != null) {
            try {
                scheduler.shutdown(true);
                logger.info("Scheduler shutdown completed");
            } catch (SchedulerException e) {
                logger.error("Error shutting down scheduler", e);
            }
        }
    }

    /**
     * Trigger increment job immediately
     */
    public void triggerIncrementJob() throws SchedulerException {
        if (scheduler != null) {
            scheduler.triggerJob(JobKey.jobKey("incrementReportJob", "reportGroup"));
            logger.info("Increment job triggered manually");
        }
    }

    /**
     * Trigger retry job immediately
     */
    public void triggerRetryJob() throws SchedulerException {
        if (scheduler != null) {
            scheduler.triggerJob(JobKey.jobKey("retryReportJob", "reportGroup"));
            logger.info("Retry job triggered manually");
        }
    }

    /**
     * Check if scheduler is running
     */
    public boolean isRunning() {
        try {
            return scheduler != null && scheduler.isStarted() && !scheduler.isShutdown();
        } catch (SchedulerException e) {
            return false;
        }
    }

    /**
     * Pause all jobs
     */
    public void pauseAll() throws SchedulerException {
        if (scheduler != null) {
            scheduler.pauseAll();
            logger.info("All jobs paused");
        }
    }

    /**
     * Resume all jobs
     */
    public void resumeAll() throws SchedulerException {
        if (scheduler != null) {
            scheduler.resumeAll();
            logger.info("All jobs resumed");
        }
    }
}
