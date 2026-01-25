package com.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event model for Volcano Engine API
 */
public class ReportEvent {

    @JsonProperty("event")
    private String event;

    @JsonProperty("params")
    private String params;

    @JsonProperty("local_time_ms")
    private Long localTimeMs;

    public ReportEvent() {
    }

    public ReportEvent(String event, String params, Long localTimeMs) {
        this.event = event;
        this.params = params;
        this.localTimeMs = localTimeMs;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public Long getLocalTimeMs() {
        return localTimeMs;
    }

    public void setLocalTimeMs(Long localTimeMs) {
        this.localTimeMs = localTimeMs;
    }

    @Override
    public String toString() {
        return "ReportEvent{" +
                "event='" + event + '\'' +
                ", params='" + params + '\'' +
                ", localTimeMs=" + localTimeMs +
                '}';
    }
}
