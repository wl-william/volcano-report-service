package com.report;

import com.report.config.AppConfig;
import com.report.config.DataSourceConfig;
import com.report.schedule.ScheduleConfig;
import com.report.service.ReportService;
import com.report.util.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application entry point for Volcano Report Service
 */
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private static ScheduleConfig scheduleConfig;

    public static void main(String[] args) {
        // Console output for debugging (in case logger fails)
        System.out.println("========================================");
        System.out.println("  Volcano Report Service Starting...");
        System.out.println("  Java Version: " + System.getProperty("java.version"));
        System.out.println("  Working Directory: " + System.getProperty("user.dir"));
        System.out.println("========================================");

        logger.info("========================================");
        logger.info("  Volcano Report Service Starting...");
        logger.info("========================================");

        try {
            // Initialize configuration
            System.out.println("[STARTUP] Loading configuration...");
            AppConfig config = AppConfig.getInstance();
            logger.info("Configuration loaded");
            System.out.println("[STARTUP] Configuration loaded successfully");

            // Test database connection with retry
            System.out.println("[STARTUP] Testing database connection...");
            DataSourceConfig dataSource = DataSourceConfig.getInstance();
            if (!testDatabaseConnectionWithRetry(dataSource, 3, 5000)) {
                System.err.println("[ERROR] Database connection failed after retries!");
                logger.error("Database connection failed after retries!");
                System.exit(1);
            }
            logger.info("Database connection OK");
            System.out.println("[STARTUP] Database connection OK");

            // Parse command line arguments
            String mode = args.length > 0 ? args[0] : "schedule";
            String date = args.length > 1 ? args[1] : ReportService.getYesterdayDate();

            switch (mode.toLowerCase()) {
                case "once":
                    // Run once for specified date and exit
                    runOnce(date);
                    break;

                case "retry":
                    // Retry mode - reprocess specified date (for manual rerun)
                    runRetry(date);
                    break;

                case "stats":
                    // Show statistics for specified date
                    showStats(date);
                    break;

                case "schedule":
                default:
                    // Start with scheduler (default)
                    startWithScheduler();
                    break;
            }

        } catch (Exception e) {
            // Print to console for debugging
            System.err.println("========================================");
            System.err.println("  APPLICATION STARTUP FAILED!");
            System.err.println("  Error: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace(System.err);

            // Also log if possible
            logger.error("Application failed to start: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Run report once for specified date and exit
     */
    private static void runOnce(String date) {
        logger.info("Running in ONCE mode for date: {}", date);
        try {
            ReportService reportService = new ReportService();
            reportService.processDate(date);
            logger.info("Report completed for {}, exiting", date);
        } finally {
            cleanup();
        }
    }

    /**
     * Retry/reprocess specified date (for manual rerun)
     */
    private static void runRetry(String date) {
        logger.info("Running in RETRY mode for date: {}", date);
        try {
            ReportService reportService = new ReportService();
            reportService.processDate(date);
            logger.info("Retry completed for {}, exiting", date);
        } finally {
            cleanup();
        }
    }

    /**
     * Show statistics for specified date
     */
    private static void showStats(String date) {
        logger.info("Running in STATS mode for date: {}", date);
        try {
            ReportService reportService = new ReportService();
            reportService.showStats(date);
        } finally {
            cleanup();
        }
    }

    /**
     * Start with scheduler
     */
    private static void startWithScheduler() {
        logger.info("Running in SCHEDULE mode");

        try {
            scheduleConfig = new ScheduleConfig();
            scheduleConfig.start();

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown signal received");
                cleanup();
            }));

            logger.info("========================================");
            logger.info("  Service started successfully!");
            logger.info("  Press Ctrl+C to stop");
            logger.info("========================================");

            // Keep main thread alive
            Thread.currentThread().join();

        } catch (Exception e) {
            logger.error("Failed to start scheduler: {}", e.getMessage(), e);
            cleanup();
            System.exit(1);
        }
    }

    /**
     * Test database connection with retry logic
     *
     * @param dataSource DataSource to test
     * @param maxAttempts Maximum number of retry attempts
     * @param delayMs Delay between retries in milliseconds
     * @return true if connection is successful, false otherwise
     */
    private static boolean testDatabaseConnectionWithRetry(DataSourceConfig dataSource,
                                                           int maxAttempts,
                                                           long delayMs) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            logger.info("Database connection attempt {}/{}", attempt, maxAttempts);
            if (dataSource.isHealthy()) {
                return true;
            }
            if (attempt < maxAttempts) {
                logger.warn("Database connection failed, retrying in {}ms...", delayMs);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Database connection retry interrupted");
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Cleanup resources
     */
    private static void cleanup() {
        logger.info("Cleaning up resources...");

        try {
            if (scheduleConfig != null) {
                scheduleConfig.shutdown();
            }

            HttpClientUtil.getInstance().close();
            DataSourceConfig.getInstance().close();

            logger.info("Cleanup completed");
        } catch (Exception e) {
            logger.error("Error during cleanup: {}", e.getMessage());
        }
    }

    /**
     * Print usage information
     */
    private static void printUsage() {
        System.out.println("Usage: java -jar volcano-report-service.jar [mode] [date]");
        System.out.println();
        System.out.println("Modes:");
        System.out.println("  schedule  - Start with scheduler (process yesterday daily, default)");
        System.out.println("  once      - Process specified date once and exit (default: yesterday)");
        System.out.println("  retry     - Reprocess specified date (default: yesterday)");
        System.out.println("  stats     - Show statistics for specified date (default: yesterday)");
        System.out.println();
        System.out.println("Date format: YYYY-MM-DD (e.g., 2026-01-25)");
        System.out.println("If date is not provided, defaults to yesterday");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar app.jar stats              # Show stats for yesterday");
        System.out.println("  java -jar app.jar stats 2026-01-20   # Show stats for specific date");
        System.out.println("  java -jar app.jar once 2026-01-20    # Process specific date once");
        System.out.println("  java -jar app.jar schedule           # Run scheduler (process yesterday daily)");
    }
}
