package com.report.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
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
            logger.info("Configuration loaded successfully");
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
}
