package com.report.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Event table configuration
 * Maps table names to their parameter fields for JSON conversion
 */
public enum EventTableConfig {
    PAGE_VIEW("page_vidw", Arrays.asList("refer_page_id", "page_id")),

    ELEMENT_CLICK("element_click", Arrays.asList("click_type", "click_position", "click_name", "click_area")),

    PAY("pay", Arrays.asList("pay_type", "pay_amount", "package_type", "package_id", "package_name", "is_ai")),

    PAY_RESULT("pay_result", Arrays.asList(
            "pay_result", "pay_type", "pay_amount", "package_type", "package_id", "package_name",
            "is_ai", "source", "device", "device_type", "sale_channel", "sd_card",
            "device_first_time", "cloud_expire_time"
    )),

    USER_INFO("user_info", Arrays.asList("reg_time", "ys_dev_cnt", "user_add_day"));

    private final String tableName;
    private final List<String> paramFields;

    // Static map for quick lookup by table name
    private static final Map<String, EventTableConfig> TABLE_MAP = new HashMap<>();

    static {
        for (EventTableConfig config : values()) {
            TABLE_MAP.put(config.tableName, config);
        }
    }

    EventTableConfig(String tableName, List<String> paramFields) {
        this.tableName = tableName;
        this.paramFields = paramFields;
    }

    public String getTableName() {
        return tableName;
    }

    public List<String> getParamFields() {
        return paramFields;
    }

    /**
     * Get event name (same as table name)
     */
    public String getEventName() {
        return tableName;
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
     * Build SQL select fields for this table
     * Returns: id, user_unique_id, event_time, report_status, {param_fields}
     */
    public String buildSelectFields() {
        StringBuilder sb = new StringBuilder();
        sb.append("id, user_unique_id, event_time, report_status");
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
