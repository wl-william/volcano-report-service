package com.report.config;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for EventTableConfig enum
 */
public class EventTableConfigTest {

    @Test
    public void testGetByTableName_ValidTables() {
        assertNotNull(EventTableConfig.getByTableName("page_vidw"));
        assertNotNull(EventTableConfig.getByTableName("element_click"));
        assertNotNull(EventTableConfig.getByTableName("pay"));
        assertNotNull(EventTableConfig.getByTableName("pay_result"));
        assertNotNull(EventTableConfig.getByTableName("user_info"));
    }

    @Test
    public void testGetByTableName_InvalidTable() {
        assertNull(EventTableConfig.getByTableName("invalid_table"));
        assertNull(EventTableConfig.getByTableName(""));
        assertNull(EventTableConfig.getByTableName(null));
    }

    @Test
    public void testIsValidTable() {
        assertTrue(EventTableConfig.isValidTable("page_vidw"));
        assertTrue(EventTableConfig.isValidTable("element_click"));
        assertTrue(EventTableConfig.isValidTable("pay"));
        assertTrue(EventTableConfig.isValidTable("pay_result"));
        assertTrue(EventTableConfig.isValidTable("user_info"));

        assertFalse(EventTableConfig.isValidTable("invalid_table"));
        assertFalse(EventTableConfig.isValidTable(""));
        assertFalse(EventTableConfig.isValidTable(null));
    }

    @Test
    public void testGetAllTableNames() {
        String[] tableNames = EventTableConfig.getAllTableNames();
        assertNotNull(tableNames);
        assertEquals(5, tableNames.length);
        assertTrue(java.util.Arrays.asList(tableNames).contains("page_vidw"));
        assertTrue(java.util.Arrays.asList(tableNames).contains("element_click"));
    }

    @Test
    public void testBuildSelectFields_PageView() {
        EventTableConfig config = EventTableConfig.PAGE_VIEW;
        String selectFields = config.buildSelectFields();
        assertNotNull(selectFields);
        assertTrue(selectFields.contains("id"));
        assertTrue(selectFields.contains("user_unique_id"));
        assertTrue(selectFields.contains("event_time"));
        assertTrue(selectFields.contains("report_status"));
        assertTrue(selectFields.contains("refer_page_id"));
        assertTrue(selectFields.contains("page_id"));
    }

    @Test
    public void testBuildPendingQuery_Pay() {
        EventTableConfig config = EventTableConfig.PAY;
        String query = config.buildPendingQuery();
        assertNotNull(query);
        assertTrue(query.contains("SELECT"));
        assertTrue(query.contains("FROM pay"));
        assertTrue(query.contains("WHERE id > ?"));
        assertTrue(query.contains("report_status = 0"));
        assertTrue(query.contains("ORDER BY id"));
        assertTrue(query.contains("LIMIT ?"));
    }

    @Test
    public void testBuildPendingCountQuery_UserInfo() {
        EventTableConfig config = EventTableConfig.USER_INFO;
        String query = config.buildPendingCountQuery();
        assertNotNull(query);
        assertTrue(query.contains("SELECT COUNT(*)"));
        assertTrue(query.contains("FROM user_info"));
        assertTrue(query.contains("WHERE report_status = 0"));
    }

    @Test
    public void testGetEventName() {
        assertEquals("page_vidw", EventTableConfig.PAGE_VIEW.getEventName());
        assertEquals("element_click", EventTableConfig.ELEMENT_CLICK.getEventName());
        assertEquals("pay", EventTableConfig.PAY.getEventName());
        assertEquals("pay_result", EventTableConfig.PAY_RESULT.getEventName());
        assertEquals("user_info", EventTableConfig.USER_INFO.getEventName());
    }

    @Test
    public void testGetParamFields_PayResult() {
        EventTableConfig config = EventTableConfig.PAY_RESULT;
        assertNotNull(config.getParamFields());
        assertTrue(config.getParamFields().size() > 0);
        assertTrue(config.getParamFields().contains("pay_result"));
        assertTrue(config.getParamFields().contains("pay_type"));
        assertTrue(config.getParamFields().contains("pay_amount"));
    }

    @Test
    public void testTableNameMatchesEventName() {
        for (EventTableConfig config : EventTableConfig.values()) {
            assertEquals(config.getTableName(), config.getEventName());
        }
    }
}
