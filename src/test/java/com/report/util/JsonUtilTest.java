package com.report.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for JsonUtil
 */
public class JsonUtilTest {

    @Test
    public void testToJson_ValidObject() {
        TestObject obj = new TestObject("test", 123);
        String json = JsonUtil.toJson(obj);
        assertNotNull(json);
        assertTrue(json.contains("test"));
        assertTrue(json.contains("123"));
    }

    @Test
    public void testToJson_NullObject() {
        String json = JsonUtil.toJson(null);
        assertNull(json);
    }

    @Test
    public void testFromJson_ValidJson() {
        String json = "{\"name\":\"test\",\"value\":123}";
        TestObject obj = JsonUtil.fromJson(json, TestObject.class);
        assertNotNull(obj);
        assertEquals("test", obj.name);
        assertEquals(123, obj.value);
    }

    @Test
    public void testFromJson_NullJson() {
        TestObject obj = JsonUtil.fromJson(null, TestObject.class);
        assertNull(obj);
    }

    @Test
    public void testFromJson_EmptyJson() {
        TestObject obj = JsonUtil.fromJson("", TestObject.class);
        assertNull(obj);
    }

    @Test(expected = RuntimeException.class)
    public void testFromJson_InvalidJson() {
        JsonUtil.fromJson("invalid json", TestObject.class);
    }

    @Test
    public void testToPrettyJson_ValidObject() {
        TestObject obj = new TestObject("test", 123);
        String json = JsonUtil.toPrettyJson(obj);
        assertNotNull(json);
        assertTrue(json.contains("test"));
        assertTrue(json.contains("\n")); // Pretty print should have newlines
    }

    @Test
    public void testMapToJson_ValidMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("key1", "value1");
        map.put("key2", 42);
        String json = JsonUtil.mapToJson(map);
        assertNotNull(json);
        assertTrue(json.contains("key1"));
        assertTrue(json.contains("value1"));
    }

    @Test
    public void testMapToJson_EmptyMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        String json = JsonUtil.mapToJson(map);
        assertEquals("{}", json);
    }

    @Test
    public void testMapToJson_NullMap() {
        String json = JsonUtil.mapToJson(null);
        assertEquals("{}", json);
    }

    /**
     * Test data class
     */
    public static class TestObject {
        public String name;
        public int value;

        public TestObject() {
        }

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }
}
