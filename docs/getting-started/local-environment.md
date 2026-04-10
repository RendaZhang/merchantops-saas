# Local Environment

## Requirements

- JDK 21
- Docker Desktop or another Docker Engine runtime with `docker compose`
- Network access to Maven Central

## Local Dependency Stack

The default `dev` profile expects these local services:

- MySQL 8.0
- Redis 7
- RabbitMQ 3 with the management UI enabled

## Environment File

Create a local `.env` file before starting containers:

```bash
cp .env.example .env
```

PowerShell:

```powershell
Copy-Item .env.example .env
```

The `.env` file is gitignored and stores local container credentials plus optional app overrides such as `MERCHANTOPS_AI_*` or `DEEPSEEK_*` values for AI live smoke. Keep real secrets only in your local `.env`.

## Start Services

```bash
docker compose up -d
docker compose ps
```

Stop services:

```bash
docker compose down
```

Stop and remove volumes:

```bash
docker compose down -v
```

## Network Model

- `docker compose` creates and reuses the pinned bridge network `merchantops-infra`
- host-machine access still uses the published ports below
- containers attached to `merchantops-infra` should use service hostnames `mysql`, `redis`, and `rabbitmq`

## Default Access

- MySQL: `localhost:3306`
- Redis: `localhost:6379`
- RabbitMQ AMQP: `localhost:5672`
- RabbitMQ Management UI: `http://localhost:15672`

Credential values come from `.env`:

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `RABBITMQ_DEFAULT_USER`
- `RABBITMQ_DEFAULT_PASS`

## Dockerized API Runtime

Official container run command:

```powershell
docker run --rm --name merchantops-api-local `
  --env-file .env `
  --network merchantops-infra `
  -p 8080:8080 `
  -e MYSQL_HOST=mysql `
  -e REDIS_HOST=redis `
  -e RABBITMQ_HOST=rabbitmq `
  merchantops-api:local
```

This path keeps the API on port `8080` and expects credentials plus optional overrides from `.env`, including:

- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `RABBITMQ_DEFAULT_USER`
- `RABBITMQ_DEFAULT_PASS`
- `TZ`
- optional `JWT_SECRET`
- optional `MERCHANTOPS_AI_*` overrides for AI-enabled local runs

## Notes

- MySQL is required for the default `dev` profile because Flyway runs on startup.
- Redis and RabbitMQ are configured in `application-dev.yml`. Keep them available if you want local config parity with the repository defaults.
- When you start the API through dev-profile `spring-boot:run`, the main app entrypoint auto-loads the repository-root `.env`. That local bootstrap does not change `@SpringBootTest` behavior and does not search outside the repository root.
- The Dockerized API path does not auto-load the repository-root `.env`; pass it explicitly with `--env-file` and `-e`.
