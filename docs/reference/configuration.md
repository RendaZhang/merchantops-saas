# Configuration

## Active Profile

`application.yml` defaults to:

- `SPRING_PROFILES_ACTIVE=dev`

Override example:

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw -f merchantops-api/pom.xml spring-boot:run
```

## `application-dev.yml`

The `dev` profile contains local integration settings for:

- MySQL
- Redis
- RabbitMQ
- JWT

## `application.yml`

Shared application configuration currently includes:

- `spring.application.name=merchantops-saas`
- `SPRING_PROFILES_ACTIVE=dev` by default
- `springdoc.show-actuator=true`
- Swagger UI operation sorting: alphabetical
- Swagger UI tag sorting: alphabetical
- Swagger authorization persistence enabled
- Swagger try-it-out enabled by default
- import processing defaults:
  - `merchantops.import.processing.chunk-size=100`
  - `merchantops.import.processing.max-rows-per-job=1000`
  - `merchantops.import.processing.stale-processing-threshold-seconds=300`
  - `merchantops.import.processing.enqueue-recovery-batch-size=100`
  - `merchantops.import.processing.enqueue-recovery-delay-ms=300000`
  - `merchantops.import.processing.enqueue-recovery-min-age-seconds=60`

## Supported Environment Variable Overrides

- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `RABBITMQ_HOST`
- `RABBITMQ_PORT`
- `RABBITMQ_DEFAULT_USER`
- `RABBITMQ_DEFAULT_PASS`
- `RABBITMQ_VHOST`
- `JWT_SECRET`
- `JWT_EXPIRE_SECONDS`
- `IMPORT_STORAGE_LOCAL_DIR`
- `IMPORT_PROCESSING_CHUNK_SIZE`
- `IMPORT_PROCESSING_MAX_ROWS_PER_JOB`
- `IMPORT_PROCESSING_STALE_THRESHOLD_SECONDS`
- `IMPORT_PROCESSING_ENQUEUE_RECOVERY_BATCH_SIZE`
- `IMPORT_PROCESSING_ENQUEUE_RECOVERY_DELAY_MS`
- `IMPORT_PROCESSING_ENQUEUE_RECOVERY_MIN_AGE_SECONDS`

## Import Processing Controls

- `merchantops.import.storage.local-dir` controls the local filesystem root used by the current storage implementation.
- `merchantops.import.processing.chunk-size` controls how many parsed data rows the worker processes before it persists progress back to `import_job`.
- `merchantops.import.processing.max-rows-per-job` is a hard guardrail; when a file exceeds the configured data-row limit, the worker fails the job with `MAX_ROWS_EXCEEDED`.
- `merchantops.import.processing.stale-processing-threshold-seconds` controls when a `PROCESSING` job is treated as stale on message redelivery. A stale job with no recorded progress restarts from `PROCESSING`; a stale job that already has `totalCount`, `successCount`, or `failureCount` progress is failed with audit error `PROCESSING_STALE` and job summary `import job processing expired after partial progress`.
- `merchantops.import.processing.enqueue-recovery-batch-size` controls how many aged `QUEUED` jobs the scheduled recovery loop republishes in one pass.
- `merchantops.import.processing.enqueue-recovery-delay-ms` controls the fixed delay and initial delay for the scheduled queued-job recovery loop.
- `merchantops.import.processing.enqueue-recovery-min-age-seconds` controls how old a `QUEUED` job must be before it is eligible for best-effort republish by the recovery loop.
- Current defaults are intentionally conservative for Week 5: `chunk-size=100`, `max-rows-per-job=1000`, `stale-processing-threshold-seconds=300`, `enqueue-recovery-batch-size=100`, `enqueue-recovery-delay-ms=300000`, and `enqueue-recovery-min-age-seconds=60`.

## Logging

- Console logs include `requestId` from MDC.
- Local log levels are tuned down for AMQP, Redis, and Hikari internals.
