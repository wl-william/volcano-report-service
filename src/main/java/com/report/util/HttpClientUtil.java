package com.report.util;

import com.report.config.AppConfig;
import com.report.model.ReportResult;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * HTTP client utility for Volcano Engine API
 */
public class HttpClientUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);
    private static HttpClientUtil instance;
    private final CloseableHttpClient httpClient;
    private final AppConfig config;

    private static final String CONTENT_TYPE = "application/json";
    private static final String HEADER_APP_KEY = "X-MCS-AppKey";

    private HttpClientUtil() {
        this.config = AppConfig.getInstance();

        // Connection pool configuration
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(50);

        // Request timeout configuration
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(config.getHttpConnectTimeout())
                .setSocketTimeout(config.getHttpSocketTimeout())
                .setConnectionRequestTimeout(config.getHttpConnectionRequestTimeout())
                .build();

        this.httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        logger.info("HttpClient initialized with connection pool");
    }

    public static synchronized HttpClientUtil getInstance() {
        if (instance == null) {
            instance = new HttpClientUtil();
        }
        return instance;
    }

    /**
     * Send POST request to Volcano Engine API
     *
     * @param endpoint API endpoint (e.g., /v2/event/json or /v2/event/list)
     * @param jsonBody JSON request body
     * @return ReportResult
     */
    public ReportResult post(String endpoint, String jsonBody) {
        String url = config.getApiBaseUrl() + endpoint;
        HttpPost httpPost = new HttpPost(url);

        // Set headers
        httpPost.setHeader("Content-Type", CONTENT_TYPE);
        httpPost.setHeader(HEADER_APP_KEY, config.getAppKey());

        // Set body
        httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

        logger.debug("Sending POST request to: {}", url);
        logger.debug("Request body: {}", jsonBody);

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String responseBody = entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8) : "";

            logger.debug("Response status: {}, body: {}", statusCode, responseBody);

            ReportResult result = new ReportResult();
            result.setHttpStatus(statusCode);
            result.setRawResponse(responseBody);

            if (statusCode == 200) {
                // Parse success response
                try {
                    ReportResult parsed = JsonUtil.fromJson(responseBody, ReportResult.class);
                    if (parsed != null) {
                        result.setSuccessCount(parsed.getSuccessCount());
                        result.setErrorCount(parsed.getErrorCount());
                        result.setErrorCode(parsed.getErrorCode());
                        result.setMessage(parsed.getMessage());
                    }
                    result.setSuccess(true);
                } catch (Exception e) {
                    logger.warn("Failed to parse response body, treating as success: {}", responseBody);
                    result.setSuccess(true);
                }
            } else {
                result.setSuccess(false);
                result.setErrorMessage("HTTP " + statusCode + ": " + responseBody);
                logger.error("API request failed: status={}, response={}", statusCode, responseBody);
            }

            return result;

        } catch (IOException e) {
            logger.error("HTTP request failed: {}", e.getMessage(), e);
            return ReportResult.failure(0, "Connection error: " + e.getMessage());
        }
    }

    /**
     * Close HTTP client
     */
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
                logger.info("HttpClient closed");
            }
        } catch (IOException e) {
            logger.error("Failed to close HttpClient", e);
        }
    }
}
