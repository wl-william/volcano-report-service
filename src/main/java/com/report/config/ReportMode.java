package com.report.config;

/**
 * Report mode for event tables
 */
public enum ReportMode {
    /**
     * Report records one by one (single API call per record)
     * Slower but more reliable for critical events
     */
    SINGLE,

    /**
     * Report records in batches (up to 50 records per API call)
     * Faster and more efficient for high-volume events
     */
    BATCH;

    /**
     * Parse report mode from string
     */
    public static ReportMode fromString(String mode) {
        if (mode == null || mode.trim().isEmpty()) {
            return BATCH; // default
        }

        try {
            return valueOf(mode.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return BATCH; // default on parse error
        }
    }
}
