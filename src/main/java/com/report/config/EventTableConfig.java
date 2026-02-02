package com.report.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Event table configuration
 * Maps table names to their parameter fields and report mode
 */
public enum EventTableConfig {
    // Default: BATCH mode for high-volume tables
    PAGE_VIEW("page_vidw", "page_vidw", Arrays.asList("refer_page_id", "page_id"), ReportMode.BATCH),

    ELEMENT_CLICK("element_click", "element_click", Arrays.asList("click_type", "click_position", "click_name", "click_area"), ReportMode.BATCH),

    // SINGLE mode for critical payment events (more reliable)
    PAY("pay", "pay", Arrays.asList("pay_type", "pay_amount", "package_type", "package_id", "package_name", "is_ai"), ReportMode.SINGLE),

    PAY_RESULT("pay_result", "pay_result", Arrays.asList(
            "pay_result", "pay_type", "pay_amount", "package_type", "package_id", "package_name",
            "is_ai", "source", "device", "device_type", "sale_channel", "sd_card",
            "device_first_time", "cloud_expire_time"
    ), ReportMode.SINGLE),

    // User info table uses special event name "__profile_set" (API requirement)
    USER_INFO("user_info", "__profile_set", Arrays.asList("reg_time", "ys_dev_cnt", "user_add_day"), ReportMode.BATCH);

    private final String tableName;
    private final String eventName;  // Event name for API (may differ from table name)
    private final List<String> paramFields;
    private final ReportMode defaultReportMode;

    // Static map for quick lookup by table name
    private static final Map<String, EventTableConfig> TABLE_MAP = new HashMap<>();

    // Runtime report mode overrides (from configuration file)
    private static final Map<String, ReportMode> REPORT_MODE_OVERRIDES = new HashMap<>();

    static {
        for (EventTableConfig config : values()) {
            TABLE_MAP.put(config.tableName, config);
        }
    }

    EventTableConfig(String tableName, String eventName, List<String> paramFields, ReportMode defaultReportMode) {
        this.tableName = tableName;
        this.eventName = eventName;
        this.paramFields = paramFields;
        this.defaultReportMode = defaultReportMode;
    }

    public String getTableName() {
        return tableName;
    }

    public List<String> getParamFields() {
        return paramFields;
    }

    /**
     * Get report mode for this table
     * Returns override value if set, otherwise returns default
     */
    public ReportMode getReportMode() {
        ReportMode override = REPORT_MODE_OVERRIDES.get(tableName);
        return override != null ? override : defaultReportMode;
    }

    /**
     * Get default report mode (without overrides)
     */
    public ReportMode getDefaultReportMode() {
        return defaultReportMode;
    }

    /**
     * Set report mode override for a table
     */
    public static void setReportModeOverride(String tableName, ReportMode mode) {
        if (TABLE_MAP.containsKey(tableName)) {
            REPORT_MODE_OVERRIDES.put(tableName, mode);
        }
    }

    /**
     * Clear all report mode overrides
     */
    public static void clearReportModeOverrides() {
        REPORT_MODE_OVERRIDES.clear();
    }

    /**
     * Get event name for API reporting
     * For most tables this is the same as table name,
     * but some tables (like user_info) use special event names
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Get configuration by table name
     */
    public static EventTableConfig getByTableName(String tableName) {
        return TABLE_MAP.get(tableName);
    }

    /**
     * Check if the table name is valid
     */
    public static boolean isValidTable(String tableName) {
        return TABLE_MAP.containsKey(tableName);
    }

    /**
     * Get all table names
     */
    public static String[] getAllTableNames() {
        return TABLE_MAP.keySet().toArray(new String[0]);
    }

    /**
     * Build SQL select fields for Hive partitioned tables
     * Returns: user_unique_id, {param_fields}
     * Note: Hive tables don't have id, event_time, or report_status fields
     */
    public String buildSelectFields() {
        StringBuilder sb = new StringBuilder();
        sb.append("user_unique_id");
        for (String field : paramFields) {
            sb.append(", ").append(field);
        }
        return sb.toString();
    }

    /**
     * Build query SQL for pending records
     */
    public String buildPendingQuery() {
        return String.format(
                "SELECT %s FROM %s WHERE id > ? AND report_status = 0 ORDER BY id LIMIT ?",
                buildSelectFields(),
                tableName
        );
    }

    /**
     * Build count SQL for pending records
     */
    public String buildPendingCountQuery() {
        return String.format(
                "SELECT COUNT(*) FROM %s WHERE report_status = 0",
                tableName
        );
    }

    /**
     * Build update status SQL
     */
    public String buildUpdateStatusQuery() {
        return String.format(
                "UPDATE %s SET report_status = ?, updated_at = NOW() WHERE id IN (%s)",
                tableName,
                "%s" // placeholder for IN clause
        );
    }
}
