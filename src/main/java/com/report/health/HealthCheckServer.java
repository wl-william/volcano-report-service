package com.report.health;

import com.report.config.DataSourceConfig;
import com.report.service.MetricsService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Simple HTTP server for health checks and metrics
 */
public class HealthCheckServer {
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckServer.class);
    private static final int DEFAULT_PORT = 8080;

    private HttpServer server;
    private final int port;

    public HealthCheckServer() {
        this(DEFAULT_PORT);
    }

    public HealthCheckServer(int port) {
        this.port = port;
    }

    /**
     * Start the health check server
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Health check endpoint
        server.createContext("/health", new HealthHandler());

        // Metrics endpoint
        server.createContext("/metrics", new MetricsHandler());

        // Readiness endpoint
        server.createContext("/ready", new ReadinessHandler());

        server.setExecutor(null); // Use default executor
        server.start();

        logger.info("Health check server started on port {}", port);
        logger.info("Endpoints: /health, /ready, /metrics");
    }

    /**
     * Stop the health check server
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("Health check server stopped");
        }
    }

    /**
     * Health endpoint handler
     * Returns 200 OK if service is running
     */
    private static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{\"status\":\"UP\",\"service\":\"volcano-report-service\"}";
            sendJsonResponse(exchange, 200, response);
        }
    }

    /**
     * Readiness endpoint handler
     * Returns 200 if service is ready to process requests (database is healthy)
     */
    private static class ReadinessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            boolean isReady = checkReadiness();

            if (isReady) {
                String response = "{\"status\":\"READY\",\"database\":\"connected\"}";
                sendJsonResponse(exchange, 200, response);
            } else {
                String response = "{\"status\":\"NOT_READY\",\"database\":\"disconnected\"}";
                sendJsonResponse(exchange, 503, response);
            }
        }

        private boolean checkReadiness() {
            try {
                DataSourceConfig dataSource = DataSourceConfig.getInstance();
                return dataSource.isHealthy();
            } catch (Exception e) {
                logger.error("Readiness check failed", e);
                return false;
            }
        }
    }

    /**
     * Metrics endpoint handler
     * Returns current metrics in plain text format
     */
    private static class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                MetricsService metrics = MetricsService.getInstance();
                String response = metrics.getMetricsSummary();
                sendTextResponse(exchange, 200, response);
            } catch (Exception e) {
                logger.error("Failed to get metrics", e);
                String response = "Error retrieving metrics: " + e.getMessage();
                sendTextResponse(exchange, 500, response);
            }
        }
    }

    /**
     * Send JSON response
     */
    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        sendResponse(exchange, statusCode, response);
    }

    /**
     * Send plain text response
     */
    private static void sendTextResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        sendResponse(exchange, statusCode, response);
    }

    /**
     * Send HTTP response
     */
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
