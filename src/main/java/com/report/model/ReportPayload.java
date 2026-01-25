package com.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Report payload model for Volcano Engine API
 */
public class ReportPayload {

    @JsonProperty("user")
    private ReportUser user;

    @JsonProperty("header")
    private Map<String, Object> header;

    @JsonProperty("events")
    private List<ReportEvent> events;

    // Database record ID for tracking
    private transient Long recordId;

    // Table name for reference
    private transient String tableName;

    public ReportPayload() {
        this.header = new HashMap<>();
        this.events = new ArrayList<>();
    }

    public ReportPayload(ReportUser user, List<ReportEvent> events) {
        this.user = user;
        this.header = new HashMap<>();
        this.events = events;
    }

    public ReportUser getUser() {
        return user;
    }

    public void setUser(ReportUser user) {
        this.user = user;
    }

    public Map<String, Object> getHeader() {
        return header;
    }

    public void setHeader(Map<String, Object> header) {
        this.header = header;
    }

    public List<ReportEvent> getEvents() {
        return events;
    }

    public void setEvents(List<ReportEvent> events) {
        this.events = events;
    }

    public void addEvent(ReportEvent event) {
        if (this.events == null) {
            this.events = new ArrayList<>();
        }
        this.events.add(event);
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public String toString() {
        return "ReportPayload{" +
                "user=" + user +
                ", events=" + events +
                ", recordId=" + recordId +
                ", tableName='" + tableName + '\'' +
                '}';
    }
}
