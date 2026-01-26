# Volcano Report Service

A robust data reporting service for Volcano Engine with batch processing, fault tolerance, and automatic retry capabilities.

## Features

- **Incremental batch reporting** - Efficiently processes large volumes of event data
- **Automatic retry** - Failed records are automatically retried with configurable retry limits
- **Checkpoint/resume support** - Can resume from the last processed position after interruption
- **Scheduled execution** - Configurable cron-based scheduling for incremental and retry jobs
- **Circuit breaker** - Protects against cascading failures to external APIs
- **Connection pooling** - Optimized database and HTTP connection management
- **Multiple event types** - Supports various event tables (page views, clicks, payments, user info)

## Prerequisites

- **Java 8+** (Java 11+ recommended)
- **MySQL 8.0+** with SSL enabled
- **Maven 3.6+**

## Quick Start

### 1. Configuration

Copy the example environment file:
```bash
cp .env.example .env
```

Edit `.env` with your actual credentials:
```properties
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password
DB_URL=jdbc:mysql://your-host:3306/your_database?useSSL=true&requireSSL=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
VOLCANO_API_KEY=your_volcano_api_key
VOLCANO_API_BASE_URL=https://gator.volces.com
```

### 2. Database Setup

Ensure your database has the required event tables with the following columns:
- `id` (primary key)
- `user_unique_id`
- `event_time`
- `report_status` (0=pending, 1=processing, 2=success, 3=failed)
- `retry_count`
- `error_msg`
- `updated_at`
- Event-specific columns as defined in `EventTableConfig`

### 3. Build

```bash
mvn clean package
```

This creates:
- `target/volcano-report-service-1.0.0.jar` - Main application JAR
- `target/lib/` - Dependencies

### 4. Run

#### Schedule Mode (Default)
Runs continuously with scheduled jobs:
```bash
java -jar target/volcano-report-service-1.0.0.jar schedule
```

#### Once Mode
Processes all pending records once and exits:
```bash
java -jar target/volcano-report-service-1.0.0.jar once
```

#### Retry Mode
Retries only failed records and exits:
```bash
java -jar target/volcano-report-service-1.0.0.jar retry
```

#### Stats Mode
Shows pending record statistics and exits:
```bash
java -jar target/volcano-report-service-1.0.0.jar stats
```

## Configuration Reference

### Database Configuration
| Property | Default | Description |
|----------|---------|-------------|
| `db.url` | - | JDBC URL with SSL enabled |
| `db.username` | - | Database username |
| `db.password` | - | Database password |
| `db.pool.size` | 10 | Maximum connection pool size |
| `db.pool.minIdle` | 5 | Minimum idle connections |

### API Configuration
| Property | Default | Description |
|----------|---------|-------------|
| `volcano.api.baseUrl` | https://gator.volces.com | Volcano Engine API base URL |
| `volcano.api.appKey` | - | Your Volcano API key |

### Batch Configuration
| Property | Default | Description |
|----------|---------|-------------|
| `batch.db.size` | 1000 | Records fetched per database batch |
| `batch.report.size` | 20 | Records sent per API request (max 50) |

### Retry Configuration
| Property | Default | Description |
|----------|---------|-------------|
| `retry.max.times` | 3 | Maximum retry attempts |
| `retry.interval.ms` | 1000 | Delay between retries (ms) |

### Schedule Configuration
| Property | Default | Description |
|----------|---------|-------------|
| `schedule.enabled` | true | Enable scheduled execution |
| `schedule.increment.cron` | `0 */5 * * * ?` | Incremental job (every 5 minutes) |
| `schedule.retry.cron` | `0 */30 * * * ?` | Retry job (every 30 minutes) |

### Event Tables
| Property | Default | Description |
|----------|---------|-------------|
| `event.tables` | page_vidw,element_click,pay,pay_result,user_info | Comma-separated list of event tables |

## Architecture

### Components

- **Application** - Main entry point with multiple run modes
- **ReportService** - Core business logic for batch processing
- **DataFetchService** - Fetches data from event tables
- **DataTransformService** - Transforms database records to API payloads
- **TaskProgressService** - Manages checkpoint state for resumability
- **HttpClientUtil** - HTTP client with circuit breaker and connection pooling
- **EventTableConfig** - Configuration for each event type

### Data Flow

1. **Fetch** - Records fetched from database in batches (checkpoint-based)
2. **Transform** - Database records transformed to Volcano API format
3. **Mark Processing** - Records marked as "processing" (status=1)
4. **Report** - Batches sent to Volcano API with retry logic
5. **Update Status** - Records marked as "success" (2) or "failed" (3)
6. **Checkpoint** - Last processed ID saved for resumption

### Fault Tolerance

- **Database retry** - 3 attempts with 5s delay on connection failure
- **API retry** - Configurable retries with exponential backoff
- **Circuit breaker** - Opens after 50% failure rate, waits 30s before retry
- **Checkpoint resume** - Can resume from last processed position after crash
- **Failed record tracking** - Failed records logged and retried separately

## Monitoring

### Log Files

- **Application logs** - Standard output with SLF4J/Logback
- **Failed records** - Separate logger named `FAILED_RECORDS`

### Metrics

The service logs key metrics:
- Total pending records per table
- Success/failure counts per batch
- Processing progress (last processed ID)
- API response times and errors

## Testing

Run unit tests:
```bash
mvn test
```

Current test coverage includes:
- JsonUtil (JSON serialization/deserialization)
- LogSanitizer (Log data sanitization)
- ReportStatus (Status enum)
- EventTableConfig (Table configuration)

## Security

- **SSL/TLS** - Database connections use SSL by default
- **Credential management** - Supports environment variables for secrets
- **Log sanitization** - Sensitive data (user IDs, API keys) sanitized in logs
- **Updated dependencies** - Uses recent versions without known CVEs

## Troubleshooting

### Database Connection Failed
- Verify MySQL is running and accessible
- Check SSL certificates are properly configured
- Ensure credentials are correct
- Review connection timeout settings

### API Request Failed
- Verify `volcano.api.appKey` is valid
- Check network connectivity to API endpoint
- Review circuit breaker status (may be open after repeated failures)

### Out of Memory
- Reduce `batch.db.size` to process smaller batches
- Increase JVM heap size: `java -Xmx2g -jar ...`

## License

Proprietary - Volcano Report Service

## Support

For issues or questions, please contact the development team.
