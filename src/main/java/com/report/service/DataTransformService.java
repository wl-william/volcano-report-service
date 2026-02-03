package com.report.service;

import com.report.config.EventTableConfig;
import com.report.model.ReportEvent;
import com.report.model.ReportPayload;
import com.report.model.ReportUser;
import com.report.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Service for transforming database records to API payload format
 */
public class DataTransformService {
    private static final Logger logger = LoggerFactory.getLogger(DataTransformService.class);

    /**
     * Transform a database record to ReportPayload
     *
     * @param tableName Table name (used as event name)
     * @param record    Database record as Map
     * @return ReportPayload for API
     */
    public ReportPayload transform(String tableName, Map<String, Object> record) {
        EventTableConfig tableConfig = EventTableConfig.getByTableName(tableName);
        if (tableConfig == null) {
            logger.error("Unknown table name: {}", tableName);
            throw new IllegalArgumentException("Unknown table name: " + tableName);
        }

        ReportPayload payload = new ReportPayload();

        // Set table name for tracking (Hive tables don't have record ID)
        payload.setRecordId(null);
        payload.setTableName(tableName);

        // Build user
        ReportUser user = new ReportUser();
        user.setUserUniqueId(getStringValue(record, "user_unique_id"));
        payload.setUser(user);

        // Build event
        ReportEvent event = new ReportEvent();
        event.setEvent(tableConfig.getEventName());  // Use event name from config (e.g., "__profile_set" for user_info)
        event.setParams(buildParamsJson(tableConfig, record));

        // Use 'et' field as event time if table has it, otherwise use current time
        if (tableConfig.hasEtField()) {
            Long etValue = getLongValue(record, "et");
            event.setLocalTimeMs(etValue != null ? etValue : System.currentTimeMillis());
        } else {
            event.setLocalTimeMs(System.currentTimeMillis());
        }

        payload.addEvent(event);

        // Header is empty by default
        payload.setHeader(new HashMap<>());

        return payload;
    }

    /**
     * Transform multiple records
     */
    public List<ReportPayload> transformBatch(String tableName, List<Map<String, Object>> records) {
        List<ReportPayload> payloads = new ArrayList<>();
        for (Map<String, Object> record : records) {
            try {
                ReportPayload payload = transform(tableName, record);
                payloads.add(payload);
            } catch (Exception e) {
                Long recordId = getLongValue(record, "id");
                logger.error("Failed to transform record {} from table {}: {}",
                        recordId, tableName, e.getMessage());
            }
        }
        return payloads;
    }

    /**
     * Build params JSON string from record fields
     * Adds report_type parameter for certain tables
     */
    private String buildParamsJson(EventTableConfig tableConfig, Map<String, Object> record) {
        Map<String, Object> params = new LinkedHashMap<>();

        // Add report_type parameter if table requires it
        if (tableConfig.needsReportType()) {
            params.put("report_type", "poc_v1");
        }

        // Add all param fields from record
        for (String field : tableConfig.getParamFields()) {
            Object value = record.get(field);
            if (value != null) {
                params.put(field, value);
            }
        }

        return JsonUtil.toJson(params);
    }

    /**
     * Get string value from record
     */
    private String getStringValue(Map<String, Object> record, String key) {
        Object value = record.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Get long value from record
     */
    private Long getLongValue(Map<String, Object> record, String key) {
        Object value = record.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Extract record IDs from payloads
     */
    public List<Long> extractRecordIds(List<ReportPayload> payloads) {
        List<Long> ids = new ArrayList<>();
        for (ReportPayload payload : payloads) {
            if (payload.getRecordId() != null) {
                ids.add(payload.getRecordId());
            }
        }
        return ids;
    }

    /**
     * Get the maximum record ID from a batch
     */
    public long getMaxRecordId(List<ReportPayload> payloads) {
        long maxId = 0;
        for (ReportPayload payload : payloads) {
            if (payload.getRecordId() != null && payload.getRecordId() > maxId) {
                maxId = payload.getRecordId();
            }
        }
        return maxId;
    }
}
