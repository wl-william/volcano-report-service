package com.report.config;

import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Application configuration loader
 */
public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static final String CONFIG_FILE = "application.properties";
    private static AppConfig instance;
    private final Properties properties;

    // Database configuration
    private String dbUrl;
    private String dbUsername;
    private String dbPassword;
    private int dbPoolSize;
    private int dbPoolMinIdle;
    private long dbPoolMaxLifetime;
    private long dbPoolConnectionTimeout;

    // Volcano API configuration
    private String apiBaseUrl;
    private String appKey;

    // Batch configuration
    private int dbBatchSize;
    private int reportBatchSize;

    // Retry configuration
    private int maxRetryTimes;
    private long retryIntervalMs;

    // HTTP configuration
    private int httpConnectTimeout;
    private int httpSocketTimeout;
    private int httpConnectionRequestTimeout;

    // Schedule configuration
    private boolean scheduleEnabled;
    private String incrementCron;
    private String retryCron;

    // Event tables
    private String[] eventTables;

    private AppConfig() {
        properties = new Properties();
        loadConfig();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    private void loadConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                logger.error("Unable to find {}", CONFIG_FILE);
                throw new RuntimeException("Configuration file not found: " + CONFIG_FILE);
            }
            properties.load(input);
            parseConfig();
            validateConfig();
            logger.info("Configuration loaded and validated successfully");
        } catch (IOException e) {
            logger.error("Error loading configuration", e);
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    private void parseConfig() {
        // Database
        dbUrl = getProperty("db.url");
        dbUsername = getProperty("db.username");
        dbPassword = getProperty("db.password");
        dbPoolSize = getIntProperty("db.pool.size", 10);
        dbPoolMinIdle = getIntProperty("db.pool.minIdle", 5);
        dbPoolMaxLifetime = getLongProperty("db.pool.maxLifetime", 1800000);
        dbPoolConnectionTimeout = getLongProperty("db.pool.connectionTimeout", 30000);

        // Volcano API
        apiBaseUrl = getProperty("volcano.api.baseUrl");
        appKey = getProperty("volcano.api.appKey");

        // Batch
        dbBatchSize = getIntProperty("batch.db.size", 1000);
        reportBatchSize = getIntProperty("batch.report.size", 20);

        // Retry
        maxRetryTimes = getIntProperty("retry.max.times", 3);
        retryIntervalMs = getLongProperty("retry.interval.ms", 1000);

        // HTTP
        httpConnectTimeout = getIntProperty("http.connect.timeout", 10000);
        httpSocketTimeout = getIntProperty("http.socket.timeout", 30000);
        httpConnectionRequestTimeout = getIntProperty("http.connection.request.timeout", 5000);

        // Schedule
        scheduleEnabled = getBooleanProperty("schedule.enabled", true);
        incrementCron = getProperty("schedule.increment.cron", "0 */5 * * * ?");
        retryCron = getProperty("schedule.retry.cron", "0 */30 * * * ?");

        // Event tables
        String tables = getProperty("event.tables", "page_vidw,element_click,pay,pay_result,user_info");
        eventTables = tables.split(",");
    }

    private String getProperty(String key) {
        return properties.getProperty(key);
    }

    private String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    private int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for {}: {}, using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    private long getLongProperty(String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid long value for {}: {}, using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    private boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    // Getters
    public String getDbUrl() { return dbUrl; }
    public String getDbUsername() { return dbUsername; }
    public String getDbPassword() { return dbPassword; }
    public int getDbPoolSize() { return dbPoolSize; }
    public int getDbPoolMinIdle() { return dbPoolMinIdle; }
    public long getDbPoolMaxLifetime() { return dbPoolMaxLifetime; }
    public long getDbPoolConnectionTimeout() { return dbPoolConnectionTimeout; }

    public String getApiBaseUrl() { return apiBaseUrl; }
    public String getAppKey() { return appKey; }

    public int getDbBatchSize() { return dbBatchSize; }
    public int getReportBatchSize() { return reportBatchSize; }

    public int getMaxRetryTimes() { return maxRetryTimes; }
    public long getRetryIntervalMs() { return retryIntervalMs; }

    public int getHttpConnectTimeout() { return httpConnectTimeout; }
    public int getHttpSocketTimeout() { return httpSocketTimeout; }
    public int getHttpConnectionRequestTimeout() { return httpConnectionRequestTimeout; }

    public boolean isScheduleEnabled() { return scheduleEnabled; }
    public String getIncrementCron() { return incrementCron; }
    public String getRetryCron() { return retryCron; }

    public String[] getEventTables() { return eventTables; }

    /**
     * Validate configuration values
     * Throws RuntimeException if validation fails
     */
    private void validateConfig() {
        List<String> errors = new ArrayList<>();

        // Validate URLs
        if (!isValidUrl(apiBaseUrl)) {
            errors.add("Invalid volcano.api.baseUrl: " + apiBaseUrl);
        }
        if (!isValidJdbcUrl(dbUrl)) {
            errors.add("Invalid db.url: " + dbUrl);
        }

        // Validate cron expressions
        if (!isValidCronExpression(incrementCron)) {
            errors.add("Invalid schedule.increment.cron: " + incrementCron);
        }
        if (!isValidCronExpression(retryCron)) {
            errors.add("Invalid schedule.retry.cron: " + retryCron);
        }

        // Validate numeric ranges
        if (dbPoolSize < 1 || dbPoolSize > 100) {
            errors.add("db.pool.size must be between 1 and 100, got: " + dbPoolSize);
        }
        if (dbPoolMinIdle < 0 || dbPoolMinIdle > dbPoolSize) {
            errors.add("db.pool.minIdle must be between 0 and db.pool.size, got: " + dbPoolMinIdle);
        }
        if (reportBatchSize < 1 || reportBatchSize > 50) {
            errors.add("batch.report.size must be between 1 and 50, got: " + reportBatchSize);
        }
        if (dbBatchSize < 1 || dbBatchSize > 10000) {
            errors.add("batch.db.size must be between 1 and 10000, got: " + dbBatchSize);
        }
        if (maxRetryTimes < 0 || maxRetryTimes > 10) {
            errors.add("retry.max.times must be between 0 and 10, got: " + maxRetryTimes);
        }

        // Validate timeouts
        if (httpConnectTimeout < 0) {
            errors.add("http.connect.timeout must be positive, got: " + httpConnectTimeout);
        }
        if (httpSocketTimeout < 0) {
            errors.add("http.socket.timeout must be positive, got: " + httpSocketTimeout);
        }

        // Validate API key is not placeholder
        if (appKey == null || appKey.trim().isEmpty() || "your_app_key".equals(appKey)) {
            logger.warn("volcano.api.appKey appears to be a placeholder value");
        }

        if (!errors.isEmpty()) {
            logger.error("Configuration validation failed with {} error(s):", errors.size());
            errors.forEach(logger::error);
            throw new RuntimeException("Invalid configuration: " + errors.size() + " error(s) found");
        }

        logger.info("Configuration validation passed");
    }

    /**
     * Validate URL format
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            new URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validate JDBC URL format
     */
    private boolean isValidJdbcUrl(String url) {
        return url != null && url.startsWith("jdbc:");
    }

    /**
     * Validate cron expression
     */
    private boolean isValidCronExpression(String cron) {
        if (cron == null || cron.trim().isEmpty()) {
            return false;
        }
        try {
            new CronExpression(cron);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
