package com.report.schedule;

import com.report.service.ReportService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduled job for processing yesterday's data daily
 * Runs at 2am every day
 */
@DisallowConcurrentExecution
public class YesterdayReportJob implements Job {
    private static final Logger logger = LoggerFactory.getLogger(YesterdayReportJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String yesterday = ReportService.getYesterdayDate();
        logger.info("Yesterday report job started for date: {}", yesterday);
        long startTime = System.currentTimeMillis();

        try {
            ReportService reportService = new ReportService();
            reportService.processDate(yesterday);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Yesterday report job completed in {}ms for date: {}", duration, yesterday);

        } catch (Exception e) {
            logger.error("Yesterday report job failed for date {}: {}", yesterday, e.getMessage(), e);
            throw new JobExecutionException("Yesterday report job failed", e);
        }
    }
}
