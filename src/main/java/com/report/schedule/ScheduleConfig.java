package com.report.schedule;

import com.report.config.AppConfig;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quartz scheduler configuration
 * Schedules daily processing of yesterday's data at 2am
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

        // Schedule yesterday report job (runs daily at 2am)
        scheduleYesterdayJob();

        scheduler.start();
        logger.info("Scheduler started");
    }

    /**
     * Schedule the yesterday report job
     * Processes yesterday's data daily
     */
    private void scheduleYesterdayJob() throws SchedulerException {
        JobDetail job = JobBuilder.newJob(YesterdayReportJob.class)
                .withIdentity("yesterdayReportJob", "reportGroup")
                .withDescription("Daily job to process yesterday's data")
                .build();

        // Use increment cron from config (default: 2am daily)
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("yesterdayTrigger", "reportGroup")
                .withSchedule(CronScheduleBuilder.cronSchedule(config.getIncrementCron())
                        .withMisfireHandlingInstructionDoNothing())
                .build();

        scheduler.scheduleJob(job, trigger);
        logger.info("Yesterday report job scheduled with cron: {}", config.getIncrementCron());
        logger.info("Job will process previous day's data daily");
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
     * Trigger yesterday job immediately
     */
    public void triggerYesterdayJob() throws SchedulerException {
        if (scheduler != null) {
            scheduler.triggerJob(JobKey.jobKey("yesterdayReportJob", "reportGroup"));
            logger.info("Yesterday job triggered manually");
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
