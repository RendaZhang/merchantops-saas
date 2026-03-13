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

The `.env` file is gitignored and stores only local credentials and ports.

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

## Notes

- MySQL is required for the default `dev` profile because Flyway runs on startup.
- Redis and RabbitMQ are configured in `application-dev.yml`. Keep them available if you want local config parity with the repository defaults.
