package com.report.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for LogSanitizer
 */
public class LogSanitizerTest {

    @Test
    public void testSanitizeUserId_ValidLongId() {
        String userId = "user123456789";
        String sanitized = LogSanitizer.sanitizeUserId(userId);
        assertEquals("user***6789", sanitized);
        assertTrue(sanitized.contains("***"));
    }

    @Test
    public void testSanitizeUserId_ShortId() {
        String userId = "short";
        String sanitized = LogSanitizer.sanitizeUserId(userId);
        assertEquals("***", sanitized);
    }

    @Test
    public void testSanitizeUserId_NullId() {
        String sanitized = LogSanitizer.sanitizeUserId(null);
        assertEquals("***", sanitized);
    }

    @Test
    public void testSanitizeUserId_EmptyId() {
        String sanitized = LogSanitizer.sanitizeUserId("");
        assertEquals("***", sanitized);
    }

    @Test
    public void testSanitizeJson_WithPassword() {
        String json = "{\"username\":\"user\",\"password\":\"secret123\"}";
        String sanitized = LogSanitizer.sanitizeJson(json);
        assertTrue(sanitized.contains("***"));
        assertFalse(sanitized.contains("secret123"));
    }

    @Test
    public void testSanitizeJson_WithAppKey() {
        String json = "{\"appKey\":\"abc123xyz\",\"data\":\"value\"}";
        String sanitized = LogSanitizer.sanitizeJson(json);
        assertTrue(sanitized.contains("***"));
        assertFalse(sanitized.contains("abc123xyz"));
        assertTrue(sanitized.contains("value")); // Non-sensitive data preserved
    }

    @Test
    public void testSanitizeJson_WithMultipleSensitiveFields() {
        String json = "{\"password\":\"pass\",\"token\":\"tok\",\"apiKey\":\"key\"}";
        String sanitized = LogSanitizer.sanitizeJson(json);
        assertFalse(sanitized.contains("pass"));
        assertFalse(sanitized.contains("tok"));
        assertFalse(sanitized.contains("key"));
    }

    @Test
    public void testSanitizeJson_NullJson() {
        String sanitized = LogSanitizer.sanitizeJson(null);
        assertNull(sanitized);
    }

    @Test
    public void testSanitizeJson_EmptyJson() {
        String sanitized = LogSanitizer.sanitizeJson("");
        assertEquals("", sanitized);
    }

    @Test
    public void testSanitizeUrl_WithSensitiveParam() {
        String url = "https://api.example.com/endpoint?key=secret123&other=value";
        String sanitized = LogSanitizer.sanitizeUrl(url);
        assertTrue(sanitized.endsWith("?***"));
        assertFalse(sanitized.contains("secret123"));
    }

    @Test
    public void testSanitizeUrl_WithoutSensitiveParam() {
        String url = "https://api.example.com/endpoint?foo=bar&baz=qux";
        String sanitized = LogSanitizer.sanitizeUrl(url);
        assertEquals(url, sanitized); // Should remain unchanged
    }

    @Test
    public void testSanitizeUrl_NoQueryString() {
        String url = "https://api.example.com/endpoint";
        String sanitized = LogSanitizer.sanitizeUrl(url);
        assertEquals(url, sanitized);
    }

    @Test
    public void testSanitizeUrl_NullUrl() {
        String sanitized = LogSanitizer.sanitizeUrl(null);
        assertNull(sanitized);
    }
}
