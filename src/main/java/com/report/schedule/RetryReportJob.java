package com.report.schedule;

import com.report.service.ReportService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduled job for retrying failed records
 */
@DisallowConcurrentExecution
public class RetryReportJob implements Job {
    private static final Logger logger = LoggerFactory.getLogger(RetryReportJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("Retry report job started");
        long startTime = System.currentTimeMillis();

        try {
            ReportService reportService = new ReportService();
            reportService.retryFailedRecords();

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Retry report job completed in {}ms", duration);

        } catch (Exception e) {
            logger.error("Retry report job failed: {}", e.getMessage(), e);
            throw new JobExecutionException("Retry report job failed", e);
        }
    }
}
