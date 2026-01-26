package com.report.util;

import com.report.config.AppConfig;
import com.report.model.ReportResult;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
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
import java.time.Duration;

/**
 * HTTP client utility for Volcano Engine API
 */
public class HttpClientUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);
    private static HttpClientUtil instance;
    private final CloseableHttpClient httpClient;
    private final AppConfig config;
    private final CircuitBreaker circuitBreaker;

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

        // Circuit breaker configuration
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open circuit if 50% of calls fail
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before trying again
                .slidingWindowSize(10) // Consider last 10 calls
                .minimumNumberOfCalls(5) // Need at least 5 calls before calculating rate
                .build();

        this.circuitBreaker = CircuitBreaker.of("volcanoApi", cbConfig);

        logger.info("HttpClient initialized with connection pool and circuit breaker");
    }

    public static synchronized HttpClientUtil getInstance() {
        if (instance == null) {
            instance = new HttpClientUtil();
        }
        return instance;
    }

    /**
     * Send POST request to Volcano Engine API with circuit breaker protection
     *
     * @param endpoint API endpoint (e.g., /v2/event/json or /v2/event/list)
     * @param jsonBody JSON request body
     * @return ReportResult
     */
    public ReportResult post(String endpoint, String jsonBody) {
        try {
            return circuitBreaker.executeSupplier(() -> doPost(endpoint, jsonBody));
        } catch (Exception e) {
            logger.error("Circuit breaker caught exception: {}", e.getMessage(), e);
            return ReportResult.failure(0, "Circuit breaker: " + e.getMessage());
        }
    }

    /**
     * Internal POST request implementation
     *
     * @param endpoint API endpoint
     * @param jsonBody JSON request body
     * @return ReportResult
     */
    private ReportResult doPost(String endpoint, String jsonBody) {
        String url = config.getApiBaseUrl() + endpoint;
        HttpPost httpPost = new HttpPost(url);

        // Set headers
        httpPost.setHeader("Content-Type", CONTENT_TYPE);
        httpPost.setHeader(HEADER_APP_KEY, config.getAppKey());

        // Set body
        httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

        logger.debug("Sending POST request to: {}", url);
        logger.debug("Request body: {}", LogSanitizer.sanitizeJson(jsonBody));

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpPost);
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
                    logger.warn("Failed to parse response body, treating as success");
                    result.setSuccess(true);
                }
            } else {
                result.setSuccess(false);
                result.setErrorMessage("HTTP " + statusCode + ": " + responseBody);
                logger.error("API request failed: status={}", statusCode);
            }

            // Ensure entity is fully consumed to release connection
            EntityUtils.consume(entity);
            return result;

        } catch (IOException e) {
            logger.error("HTTP request failed: {}", e.getMessage(), e);
            return ReportResult.failure(0, "Connection error: " + e.getMessage());
        } finally {
            // Ensure response is closed
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    logger.warn("Failed to close HTTP response", e);
                }
            }
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
