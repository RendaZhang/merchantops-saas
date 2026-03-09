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

## Logging

- Console logs include `requestId` from MDC.
- Local log levels are tuned down for AMQP, Redis, and Hikari internals.
