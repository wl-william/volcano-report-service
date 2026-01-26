package com.report.model;

/**
 * Report status enum to replace magic numbers
 */
public enum ReportStatus {
    PENDING(0, "Pending"),
    PROCESSING(1, "Processing"),
    SUCCESS(2, "Success"),
    FAILED(3, "Failed");

    private final int code;
    private final String description;

    ReportStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get ReportStatus from code
     *
     * @param code Status code
     * @return ReportStatus
     * @throws IllegalArgumentException if code is invalid
     */
    public static ReportStatus fromCode(int code) {
        for (ReportStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status code: " + code);
    }

    /**
     * Check if status code is valid
     *
     * @param code Status code to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidCode(int code) {
        try {
            fromCode(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
