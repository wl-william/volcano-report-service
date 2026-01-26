# Volcano Report Service - Architecture

## Overview

Volcano Report Service is a batch data reporting system designed to reliably transfer event data from a MySQL database to the Volcano Engine API. The architecture emphasizes fault tolerance, resumability, and efficient resource utilization.

## System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Application Layer                     │
│  ┌─────────────┬──────────────┬────────────────────┐   │
│  │ Once Mode   │ Retry Mode   │ Schedule Mode      │   │
│  │ (One-time)  │ (Retry only) │ (Continuous)       │   │
│  └──────┬──────┴──────┬───────┴──────────┬─────────┘   │
│         │             │                   │             │
│         └─────────────┴───────────────────┘             │
│                       │                                 │
└───────────────────────┼─────────────────────────────────┘
                        │
┌───────────────────────┼─────────────────────────────────┐
│               Service Layer                              │
│  ┌────────────────────┴───────────────────────┐        │
│  │          ReportService                      │        │
│  │  - executeFullReport()                      │        │
│  │  - processTable()                           │        │
│  │  - reportBatchWithRetry()                   │        │
│  │  - handleFailedBatch()                      │        │
│  └────┬───────────────────┬─────────────┬──────┘        │
│       │                   │             │               │
│  ┌────┴─────┐   ┌────────┴───┐   ┌────┴──────────┐   │
│  │ DataFetch│   │ DataTransf │   │ TaskProgress  │   │
│  │ Service  │   │ Service    │   │ Service       │   │
│  └────┬─────┘   └────────────┘   └───────────────┘   │
└───────┼──────────────────────────────────────────────────┘
        │
┌───────┼──────────────────────────────────────────────────┐
│  Repository & Util Layer                                 │
│  ┌────┴──────────────┐   ┌─────────────────┐           │
│  │ EventDataRepo     │   │ HttpClientUtil  │           │
│  │ - fetchPending    │   │ - post()        │           │
│  │ - updateStatus    │   │ - Circuit Breaker│          │
│  └────┬──────────────┘   └────────┬────────┘           │
└───────┼───────────────────────────┼─────────────────────┘
        │                           │
┌───────┴─────────┐       ┌─────────┴──────────┐
│  MySQL Database │       │  Volcano Engine    │
│  - Event Tables │       │  API               │
└─────────────────┘       └────────────────────┘
```

## Core Components

### 1. Application Layer

**Purpose**: Entry point and execution mode selection

**Responsibilities**:
- Parse command-line arguments
- Initialize configuration and services
- Manage application lifecycle
- Handle graceful shutdown

**Modes**:
- **schedule**: Runs continuously with Quartz scheduler
- **once**: Single execution of all pending records
- **retry**: Retry only failed records
- **stats**: Display statistics without processing

### 2. Service Layer

#### ReportService

**Purpose**: Core business logic for batch processing

**Key Methods**:
- `executeFullReport()`: Process all event tables
- `processTable()`: Process single table with checkpointing
- `reportBatchWithRetry()`: Send batch to API with retry logic
- `handleFailedBatch()`: Log and mark failed records

**Features**:
- Checkpoint-based processing (resume from last ID)
- Batch size management (configurable)
- Retry with exponential backoff
- Failed record tracking

#### DataFetchService

**Purpose**: Data access abstraction

**Responsibilities**:
- Fetch pending records from database
- Update record status (pending/processing/success/failed)
- Get statistics on pending records
- Validate table names

#### DataTransformService

**Purpose**: Data transformation between database and API formats

**Responsibilities**:
- Transform database rows to ReportPayload objects
- Build params JSON from event-specific fields
- Extract record IDs for batch processing
- Map user and event information

#### TaskProgressService

**Purpose**: Checkpoint management for resumability

**Responsibilities**:
- Track last processed ID per table
- Maintain success/failure counts
- Support task pause and resume
- Initialize progress tracking table

### 3. Repository & Utility Layer

#### EventDataRepository

**Purpose**: Direct database operations

**Responsibilities**:
- Execute SQL queries for event tables
- Batch status updates
- Handle database transactions
- Dynamic query building

**Key Features**:
- PreparedStatement for SQL injection prevention
- Batch operations for performance
- Connection pooling (HikariCP)

#### HttpClientUtil

**Purpose**: HTTP client for Volcano API

**Features**:
- Connection pooling (100 max total, 50 per route)
- Circuit breaker pattern (Resilience4j)
- Configurable timeouts
- Automatic retry handling
- Request/response logging

**Circuit Breaker Configuration**:
- Failure threshold: 50%
- Open state duration: 30 seconds
- Sliding window: 10 calls
- Minimum calls: 5

### 4. Configuration Layer

#### AppConfig

**Purpose**: Application configuration management

**Features**:
- Load from `application.properties`
- Environment variable overrides
- Configuration validation
- Type-safe property access

#### EventTableConfig

**Purpose**: Event type configuration

**Features**:
- Enum-based table definitions
- SQL query generation
- Field mapping per event type
- Table name validation

## Data Flow

### Normal Processing Flow

```
1. Fetch Batch
   ├─ Query: WHERE id > lastProcessedId AND report_status = 0
   ├─ Limit: batch.db.size (default 1000)
   └─ Order: By ID ascending

2. Transform
   ├─ Map database columns to ReportPayload
   ├─ Build params JSON from event-specific fields
   └─ Extract user information

3. Mark Processing
   ├─ Update: SET report_status = 1
   └─ Prevents duplicate processing

4. Report to API
   ├─ Batch size: batch.report.size (default 20)
   ├─ Endpoint: /v2/event/list
   ├─ Retry: Up to retry.max.times attempts
   └─ Circuit breaker protection

5. Update Status
   ├─ Success: SET report_status = 2
   ├─ Failed: SET report_status = 3, retry_count++
   └─ Store error message

6. Update Checkpoint
   ├─ Save last processed ID
   ├─ Update success/fail counts
   └─ Commit task progress
```

### Retry Flow

```
1. Fetch Failed Records
   ├─ Query: WHERE report_status = 3 AND retry_count < max
   └─ Order: By ID ascending

2. Transform & Report
   ├─ Same as normal flow
   └─ No checkpoint update (use record ID)

3. Update Status
   ├─ Success: SET report_status = 2
   └─ Failed: retry_count++ (eventually gives up)
```

## Fault Tolerance

### Database Connection Retry

- **Attempts**: 3
- **Delay**: 5 seconds between attempts
- **Purpose**: Handle temporary database unavailability

### API Retry

- **Attempts**: Configurable (default 3)
- **Delay**: Configurable (default 1000ms)
- **Backoff**: Linear (could be improved to exponential)

### Circuit Breaker

- **Trigger**: 50% failure rate over 10 calls
- **Recovery**: 30-second wait before half-open state
- **Purpose**: Prevent cascade failures, give API time to recover

### Checkpoint Resume

- **Tracking**: Last processed ID per table/task type
- **Storage**: task_progress table in database
- **Benefit**: Can resume after crash without reprocessing

### Resource Leak Prevention

- **HTTP**: CloseableHttpResponse properly closed
- **Database**: Try-with-resources for Connection/Statement/ResultSet
- **Entity consumption**: EntityUtils.consume() called explicitly

## Security

### Data Protection

- **SSL/TLS**: Database connections use SSL
- **Log sanitization**: User IDs and API keys masked in logs
- **Environment variables**: Credentials not in code/config

### Input Validation

- **Table names**: Validated against enum whitelist
- **Status codes**: Validated against enum
- **Configuration**: Validated on startup (URLs, cron, ranges)

### SQL Injection Prevention

- **PreparedStatement**: All user-controlled parameters
- **Table names**: Validated against enum (no direct user input)

## Performance Optimization

### Database

- **Connection pooling**: HikariCP with 10 connections
- **Batch operations**: Bulk status updates
- **Prepared statement caching**: MySQL-specific optimizations
- **Indexed queries**: Query by ID (primary key)

### HTTP

- **Connection pooling**: 100 max total, 50 per route
- **Keep-alive**: Reuse connections
- **Timeouts**: Prevent hanging requests
  - Connect: 10s
  - Socket: 30s
  - Request: 5s

### Memory

- **Streaming**: Process in batches, not all at once
- **Batch size**: Configurable (db: 1000, api: 20)
- **No caching**: Minimal memory footprint

## Monitoring & Observability

### Logging

- **Framework**: SLF4J + Logback
- **Levels**: INFO, DEBUG, WARN, ERROR
- **Structured**: Key-value pairs for parsing

**Log Categories**:
- Application lifecycle events
- Batch processing progress
- API request/response (with sanitization)
- Failed records (separate logger)
- Database connection health

### Metrics (Recommended)

- Total pending records per table
- Processing rate (records/minute)
- API success/failure rates
- Retry counts
- Circuit breaker state changes

## Scalability Considerations

### Vertical Scaling

- Increase `db.pool.size` for more concurrent database operations
- Increase `batch.db.size` for larger database batches
- Tune JVM heap size for larger volumes

### Horizontal Scaling

Current architecture is **single-instance**. For horizontal scaling:

**Challenges**:
- Checkpoint state in database (not distributed)
- No coordination between instances
- Risk of duplicate processing

**Possible Solutions**:
- Partition tables by ID range or hash
- Use distributed locking (Redis, ZooKeeper)
- Implement leader election
- Shard by event type (different instances handle different tables)

## Deployment

### Prerequisites

- Java 8+ runtime
- MySQL 8.0+ with SSL configured
- Network access to Volcano Engine API
- Sufficient disk space for logs

### Production Recommendations

1. **Environment Variables**: Use for all secrets
2. **SSL Certificates**: Properly configured for MySQL
3. **Monitoring**: Implement metrics collection
4. **Alerting**: Failed batches, high error rates
5. **Log Rotation**: Prevent disk space issues
6. **Health Checks**: HTTP endpoint for load balancer
7. **Graceful Shutdown**: SIGTERM handling (already implemented)

### Configuration Tuning

**For high volume**:
- Increase `batch.db.size` to 5000-10000
- Increase `batch.report.size` to 50 (API max)
- Increase `db.pool.size` to 20-50
- Use faster schedule (every 1-2 minutes)

**For reliability**:
- Decrease batch sizes for faster failure detection
- Increase retry attempts
- Add exponential backoff
- Implement dead letter queue

## Future Improvements

1. **Metrics**: Integrate Micrometer for metrics
2. **Health Check**: HTTP endpoint for monitoring
3. **Async Processing**: Non-blocking HTTP clients
4. **Exponential Backoff**: Better retry strategy
5. **Dead Letter Queue**: For permanently failed records
6. **Distributed Tracing**: OpenTelemetry integration
7. **Horizontal Scaling**: Distributed coordination
8. **Event Streaming**: Kafka/Pulsar for real-time processing
