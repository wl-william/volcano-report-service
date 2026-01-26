package com.report.util;

/**
 * Utility class for sanitizing sensitive data in logs
 */
public class LogSanitizer {

    private LogSanitizer() {
        // Utility class, prevent instantiation
    }

    /**
     * Sanitize user ID for logging
     * Keeps first 4 and last 4 characters, masks the middle
     *
     * @param userId User ID to sanitize
     * @return Sanitized user ID
     */
    public static String sanitizeUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return "***";
        }
        if (userId.length() <= 8) {
            return "***";
        }
        return userId.substring(0, 4) + "***" + userId.substring(userId.length() - 4);
    }

    /**
     * Sanitize JSON string by redacting sensitive fields
     * Redacts: password, appKey, token, secret, apiKey
     *
     * @param json JSON string to sanitize
     * @return Sanitized JSON string
     */
    public static String sanitizeJson(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        return json
                .replaceAll("(\"password\"\\s*:\\s*\")[^\"]*", "$1***")
                .replaceAll("(\"appKey\"\\s*:\\s*\")[^\"]*", "$1***")
                .replaceAll("(\"token\"\\s*:\\s*\")[^\"]*", "$1***")
                .replaceAll("(\"secret\"\\s*:\\s*\")[^\"]*", "$1***")
                .replaceAll("(\"apiKey\"\\s*:\\s*\")[^\"]*", "$1***")
                .replaceAll("(\"api_key\"\\s*:\\s*\")[^\"]*", "$1***");
    }

    /**
     * Sanitize URL by removing query parameters that might contain sensitive data
     *
     * @param url URL to sanitize
     * @return Sanitized URL
     */
    public static String sanitizeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        // Remove query string if it contains sensitive parameter names
        if (url.contains("?") &&
            (url.contains("key=") || url.contains("token=") ||
             url.contains("password=") || url.contains("secret="))) {
            return url.substring(0, url.indexOf("?")) + "?***";
        }

        return url;
    }
}
