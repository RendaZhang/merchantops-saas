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

## Import Processing Controls

- `merchantops.import.storage.local-dir` controls the local filesystem root used by the current storage implementation.
- `merchantops.import.processing.chunk-size` controls how many parsed data rows the worker processes before it persists progress back to `import_job`.
- `merchantops.import.processing.max-rows-per-job` is a hard guardrail; when a file exceeds the configured data-row limit, the worker fails the job with `MAX_ROWS_EXCEEDED`.
- Current defaults are intentionally conservative for Week 5: `chunk-size=100` and `max-rows-per-job=1000`.

## Logging

- Console logs include `requestId` from MDC.
- Local log levels are tuned down for AMQP, Redis, and Hikari internals.
