package com.report.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Service for collecting and exposing application metrics
 */
public class MetricsService {
    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);
    private static MetricsService instance;

    private final MeterRegistry registry;

    // Counters
    private final Counter recordsProcessedCounter;
    private final Counter recordsSuccessCounter;
    private final Counter recordsFailedCounter;
    private final Counter apiBatchSentCounter;
    private final Counter apiBatchSuccessCounter;
    private final Counter apiBatchFailedCounter;
    private final Counter circuitBreakerOpenCounter;

    // Timers
    private final Timer batchProcessingTimer;
    private final Timer apiRequestTimer;
    private final Timer databaseQueryTimer;

    private MetricsService() {
        this.registry = new SimpleMeterRegistry();

        // Initialize counters
        this.recordsProcessedCounter = Counter.builder("volcano.records.processed")
                .description("Total number of records processed")
                .register(registry);

        this.recordsSuccessCounter = Counter.builder("volcano.records.success")
                .description("Total number of successfully reported records")
                .register(registry);

        this.recordsFailedCounter = Counter.builder("volcano.records.failed")
                .description("Total number of failed records")
                .register(registry);

        this.apiBatchSentCounter = Counter.builder("volcano.api.batches.sent")
                .description("Total number of API batches sent")
                .register(registry);

        this.apiBatchSuccessCounter = Counter.builder("volcano.api.batches.success")
                .description("Total number of successful API batches")
                .register(registry);

        this.apiBatchFailedCounter = Counter.builder("volcano.api.batches.failed")
                .description("Total number of failed API batches")
                .register(registry);

        this.circuitBreakerOpenCounter = Counter.builder("volcano.circuit.breaker.open")
                .description("Number of times circuit breaker opened")
                .register(registry);

        // Initialize timers
        this.batchProcessingTimer = Timer.builder("volcano.batch.processing.time")
                .description("Time taken to process a batch")
                .register(registry);

        this.apiRequestTimer = Timer.builder("volcano.api.request.time")
                .description("Time taken for API requests")
                .register(registry);

        this.databaseQueryTimer = Timer.builder("volcano.database.query.time")
                .description("Time taken for database queries")
                .register(registry);

        logger.info("MetricsService initialized");
    }

    public static synchronized MetricsService getInstance() {
        if (instance == null) {
            instance = new MetricsService();
        }
        return instance;
    }

    public MeterRegistry getRegistry() {
        return registry;
    }

    // Record methods
    public void recordProcessed(long count) {
        recordsProcessedCounter.increment(count);
    }

    public void recordSuccess(long count) {
        recordsSuccessCounter.increment(count);
    }

    public void recordFailed(long count) {
        recordsFailedCounter.increment(count);
    }

    public void recordApiBatchSent() {
        apiBatchSentCounter.increment();
    }

    public void recordApiBatchSuccess() {
        apiBatchSuccessCounter.increment();
    }

    public void recordApiBatchFailed() {
        apiBatchFailedCounter.increment();
    }

    public void recordCircuitBreakerOpen() {
        circuitBreakerOpenCounter.increment();
    }

    public void recordBatchProcessingTime(long durationMs) {
        batchProcessingTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordApiRequestTime(long durationMs) {
        apiRequestTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordDatabaseQueryTime(long durationMs) {
        databaseQueryTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Get current metrics summary
     */
    public String getMetricsSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== Metrics Summary ==========\n");
        sb.append(String.format("Records Processed: %.0f\n", recordsProcessedCounter.count()));
        sb.append(String.format("Records Success: %.0f\n", recordsSuccessCounter.count()));
        sb.append(String.format("Records Failed: %.0f\n", recordsFailedCounter.count()));
        sb.append(String.format("API Batches Sent: %.0f\n", apiBatchSentCounter.count()));
        sb.append(String.format("API Batches Success: %.0f\n", apiBatchSuccessCounter.count()));
        sb.append(String.format("API Batches Failed: %.0f\n", apiBatchFailedCounter.count()));
        sb.append(String.format("Circuit Breaker Opens: %.0f\n", circuitBreakerOpenCounter.count()));

        if (batchProcessingTimer.count() > 0) {
            sb.append(String.format("Avg Batch Processing Time: %.2f ms\n",
                    batchProcessingTimer.mean(TimeUnit.MILLISECONDS)));
        }
        if (apiRequestTimer.count() > 0) {
            sb.append(String.format("Avg API Request Time: %.2f ms\n",
                    apiRequestTimer.mean(TimeUnit.MILLISECONDS)));
        }
        if (databaseQueryTimer.count() > 0) {
            sb.append(String.format("Avg Database Query Time: %.2f ms\n",
                    databaseQueryTimer.mean(TimeUnit.MILLISECONDS)));
        }
        sb.append("=====================================\n");
        return sb.toString();
    }

    /**
     * Log current metrics
     */
    public void logMetrics() {
        logger.info(getMetricsSummary());
    }

    /**
     * Reset all metrics (useful for testing)
     */
    public void reset() {
        registry.clear();
    }
}
