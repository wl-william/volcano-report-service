package com.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * User model for Volcano Engine API
 */
public class ReportUser {

    @JsonProperty("user_unique_id")
    private String userUniqueId;

    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("web_id")
    private String webId;

    public ReportUser() {
    }

    public ReportUser(String userUniqueId) {
        this.userUniqueId = userUniqueId;
    }

    public String getUserUniqueId() {
        return userUniqueId;
    }

    public void setUserUniqueId(String userUniqueId) {
        this.userUniqueId = userUniqueId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getWebId() {
        return webId;
    }

    public void setWebId(String webId) {
        this.webId = webId;
    }

    @Override
    public String toString() {
        return "ReportUser{" +
                "userUniqueId='" + userUniqueId + '\'' +
                '}';
    }
}
