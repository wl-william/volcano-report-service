package com.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * API response result model
 */
public class ReportResult {

    @JsonProperty("e")
    private Integer errorCode;

    @JsonProperty("message")
    private String message;

    @JsonProperty("sc")
    private Integer successCount;

    @JsonProperty("ec")
    private Integer errorCount;

    // HTTP status code
    private int httpStatus;

    // Raw response body
    private String rawResponse;

    // Request success flag
    private boolean success;

    // Error message for failed requests
    private String errorMessage;

    public ReportResult() {
    }

    public static ReportResult success(int successCount) {
        ReportResult result = new ReportResult();
        result.setSuccess(true);
        result.setHttpStatus(200);
        result.setSuccessCount(successCount);
        result.setErrorCount(0);
        return result;
    }

    public static ReportResult failure(int httpStatus, String errorMessage) {
        ReportResult result = new ReportResult();
        result.setSuccess(false);
        result.setHttpStatus(httpStatus);
        result.setErrorMessage(errorMessage);
        return result;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "ReportResult{" +
                "success=" + success +
                ", httpStatus=" + httpStatus +
                ", successCount=" + successCount +
                ", errorCount=" + errorCount +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
