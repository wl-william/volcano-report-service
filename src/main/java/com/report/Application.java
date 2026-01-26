package com.report;

import com.report.config.AppConfig;
import com.report.config.DataSourceConfig;
import com.report.schedule.ScheduleConfig;
import com.report.service.ReportService;
import com.report.service.TaskProgressService;
import com.report.util.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

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

            // Initialize task progress table
            TaskProgressService progressService = new TaskProgressService();
            progressService.initTable();

            // Parse command line arguments
            String mode = args.length > 0 ? args[0] : "schedule";

            switch (mode.toLowerCase()) {
                case "once":
                    // Run once and exit
                    runOnce();
                    break;

                case "retry":
                    // Retry failed records only
                    runRetry();
                    break;

                case "stats":
                    // Show statistics only
                    showStats();
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
     * Run report once and exit
     */
    private static void runOnce() {
        logger.info("Running in ONCE mode");
        try {
            ReportService reportService = new ReportService();
            reportService.executeFullReport();
            logger.info("Report completed, exiting");
        } finally {
            cleanup();
        }
    }

    /**
     * Run retry only
     */
    private static void runRetry() {
        logger.info("Running in RETRY mode");
        try {
            ReportService reportService = new ReportService();
            reportService.retryFailedRecords();
            logger.info("Retry completed, exiting");
        } finally {
            cleanup();
        }
    }

    /**
     * Show statistics only
     */
    private static void showStats() {
        logger.info("Running in STATS mode");
        try {
            ReportService reportService = new ReportService();
            Map<String, Long> stats = reportService.getStatistics();

            System.out.println("\n========== Pending Records Statistics ==========");
            long total = 0;
            for (Map.Entry<String, Long> entry : stats.entrySet()) {
                System.out.printf("  %-20s : %d%n", entry.getKey(), entry.getValue());
                total += entry.getValue();
            }
            System.out.println("------------------------------------------------");
            System.out.printf("  %-20s : %d%n", "TOTAL", total);
            System.out.println("================================================\n");
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
        System.out.println("Usage: java -jar volcano-report-service.jar [mode]");
        System.out.println();
        System.out.println("Modes:");
        System.out.println("  schedule  - Start with scheduler (default)");
        System.out.println("  once      - Run report once and exit");
        System.out.println("  retry     - Retry failed records and exit");
        System.out.println("  stats     - Show statistics and exit");
    }
}
