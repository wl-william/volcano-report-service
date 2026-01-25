package com.report.schedule;

import com.report.service.ReportService;
import com.report.service.TaskProgressService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduled job for incremental data reporting
 */
@DisallowConcurrentExecution
public class IncrementReportJob implements Job {
    private static final Logger logger = LoggerFactory.getLogger(IncrementReportJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("Increment report job started");
        long startTime = System.currentTimeMillis();

        try {
            TaskProgressService progressService = new TaskProgressService();

            // Check if there are any running tasks (resume from checkpoint)
            if (progressService.hasRunningTasks()) {
                logger.info("Found running tasks, will resume from checkpoint");
            }

            ReportService reportService = new ReportService();
            reportService.executeFullReport();

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Increment report job completed in {}ms", duration);

        } catch (Exception e) {
            logger.error("Increment report job failed: {}", e.getMessage(), e);
            throw new JobExecutionException("Increment report job failed", e);
        }
    }
}
