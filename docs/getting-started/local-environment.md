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

The tracked `.env.example` also includes a local-only `JWT_SECRET` so the production-like runtime compose path can start after copying the example file. Replace that value in any real deployment and inject the secret from the runtime environment.

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

## Same-Origin Admin Runtime

Productization Baseline Slice C adds a production-like local runtime overlay for API plus admin web:

```powershell
docker compose -f docker-compose.yml -f docker-compose.runtime.yml up -d --build
```

This starts:

- infra services from `docker-compose.yml`
- `merchantops-api-runtime` on `http://localhost:8080`
- `merchantops-admin-web-runtime` on `http://localhost:8081`

The admin web container serves the built Vite app through Nginx and proxies `/api/...` to the API container over the `merchantops-infra` network. Browser traffic stays same-origin at `http://localhost:8081`.

Stop the production-like runtime without removing data:

```powershell
docker compose -f docker-compose.yml -f docker-compose.runtime.yml down
```

## Notes

- MySQL is required for the default `dev` profile because Flyway runs on startup.
- Redis and RabbitMQ are configured in `application-dev.yml`. Keep them available if you want local config parity with the repository defaults.
- When you start the API through dev-profile `spring-boot:run`, the main app entrypoint auto-loads the repository-root `.env`. That local bootstrap does not change `@SpringBootTest` behavior and does not search outside the repository root.
- Dockerized API and runtime compose paths do not auto-load the repository-root `.env`; pass env values explicitly with `--env-file`, compose `env_file`, `-e`, or platform-provided environment variables.
- Use [../runbooks/deployment-runtime-smoke-test.md](../runbooks/deployment-runtime-smoke-test.md) when validating the same-origin admin + API runtime path.
