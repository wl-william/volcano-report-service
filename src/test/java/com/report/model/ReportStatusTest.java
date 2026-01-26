package com.report.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for ReportStatus enum
 */
public class ReportStatusTest {

    @Test
    public void testPendingStatus() {
        assertEquals(0, ReportStatus.PENDING.getCode());
        assertEquals("Pending", ReportStatus.PENDING.getDescription());
    }

    @Test
    public void testProcessingStatus() {
        assertEquals(1, ReportStatus.PROCESSING.getCode());
        assertEquals("Processing", ReportStatus.PROCESSING.getDescription());
    }

    @Test
    public void testSuccessStatus() {
        assertEquals(2, ReportStatus.SUCCESS.getCode());
        assertEquals("Success", ReportStatus.SUCCESS.getDescription());
    }

    @Test
    public void testFailedStatus() {
        assertEquals(3, ReportStatus.FAILED.getCode());
        assertEquals("Failed", ReportStatus.FAILED.getDescription());
    }

    @Test
    public void testFromCode_ValidCodes() {
        assertEquals(ReportStatus.PENDING, ReportStatus.fromCode(0));
        assertEquals(ReportStatus.PROCESSING, ReportStatus.fromCode(1));
        assertEquals(ReportStatus.SUCCESS, ReportStatus.fromCode(2));
        assertEquals(ReportStatus.FAILED, ReportStatus.fromCode(3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromCode_InvalidCode() {
        ReportStatus.fromCode(999);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromCode_NegativeCode() {
        ReportStatus.fromCode(-1);
    }

    @Test
    public void testIsValidCode_ValidCodes() {
        assertTrue(ReportStatus.isValidCode(0));
        assertTrue(ReportStatus.isValidCode(1));
        assertTrue(ReportStatus.isValidCode(2));
        assertTrue(ReportStatus.isValidCode(3));
    }

    @Test
    public void testIsValidCode_InvalidCodes() {
        assertFalse(ReportStatus.isValidCode(4));
        assertFalse(ReportStatus.isValidCode(-1));
        assertFalse(ReportStatus.isValidCode(999));
    }

    @Test
    public void testValues_ContainsAllStatuses() {
        ReportStatus[] statuses = ReportStatus.values();
        assertEquals(4, statuses.length);
        assertTrue(java.util.Arrays.asList(statuses).contains(ReportStatus.PENDING));
        assertTrue(java.util.Arrays.asList(statuses).contains(ReportStatus.PROCESSING));
        assertTrue(java.util.Arrays.asList(statuses).contains(ReportStatus.SUCCESS));
        assertTrue(java.util.Arrays.asList(statuses).contains(ReportStatus.FAILED));
    }
}
