# MerchantOps SaaS

MerchantOps SaaS is a multi-tenant operations support platform for cross-border seller teams.  
It includes a backend skeleton for ticket handling, async import workflows, billing traceability, feature flags, audit logs, and foundational engineering practices.

## Contents

- Target Users
- Core Goals
- Repository Structure
- Module Responsibilities
- Requirements & Version Policy
- Local Dependencies (Docker Compose)
- Quick Start
- Health Checks
- Troubleshooting
- Current Status

## Target Users

- Cross-border e-commerce seller teams
- Platform operators
- Merchant administrators
- Internal support and customer service teams

## Core Goals

- Build a realistic multi-tenant SaaS backend
- Support interview storytelling and resume presentation
- Provide a solid GitHub portfolio project

## Repository Structure

```text
merchantops-saas/
├── .mvn/
├── merchantops-api/
├── merchantops-common/
├── merchantops-domain/
├── merchantops-infra/
├── deploy/
├── docs/
├── scripts/
├── sql/
├── CHANGELOG.md
├── README.md
├── docker-compose.yml
├── mvnw
├── mvnw.cmd
└── pom.xml
```

## Module Responsibilities

- `merchantops-api`: REST controllers, DTOs, and API entry points
- `merchantops-domain`: business models, domain services, and business rules
- `merchantops-infra`: persistence, integration adapters, and infrastructure concerns
- `merchantops-common`: shared responses, exceptions, and utility classes

## Requirements & Version Policy

- Runtime recommendation: Java 17+
- Build target: Java 21
- Local build requirement: JDK 21
- Build tool: Maven 3.9.x (or Maven Wrapper)
- Frameworks: Spring Boot 3.3.8, Spring Web, Spring Validation, Spring Boot Actuator
- Network requirement: access to Maven Central (`https://repo.maven.apache.org`)

## Local Dependencies (Docker Compose)

This repository includes a local dependency stack for development:

- MySQL 8.0
- Redis 7
- RabbitMQ 3 (management UI enabled)

Before starting services, create your local environment file:

```bash
cp .env.example .env
```

Windows Command Prompt / PowerShell:

```powershell
Copy-Item .env.example .env
```

The `.env` file is gitignored and used for local credentials.

Start services:

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

Default local access:

- MySQL: `localhost:3306`, database/user/password are read from `.env` (`MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`; root password from `MYSQL_ROOT_PASSWORD`)
- Redis: `localhost:6379`
- RabbitMQ AMQP: `localhost:5672`
- RabbitMQ Management UI: `http://localhost:15672` (credentials from `.env`: `RABBITMQ_DEFAULT_USER` / `RABBITMQ_DEFAULT_PASS`)

## Quick Start

1. Start local dependencies (optional for now, recommended for future integration work):

```bash
cp .env.example .env
docker compose up -d
```

2. Build and install required modules from the repository root:

```bash
./mvnw -pl merchantops-api -am -DskipTests install
```

3. Start the API module using its own POM (avoids `spring-boot` prefix resolution issues on the aggregator root):

```bash
./mvnw -f merchantops-api/pom.xml spring-boot:run
```

Windows Command Prompt / PowerShell equivalents:

```powershell
mvnw.cmd -pl merchantops-api -am -DskipTests install
mvnw.cmd -f merchantops-api/pom.xml spring-boot:run
```

## Health Checks

```bash
curl -s http://localhost:8080/health
# expected: {"status":"UP","service":"merchantops-saas"}

curl -s http://localhost:8080/actuator/health
# expected: {"status":"UP"} (may include additional fields)
```

## Troubleshooting

- If `docker` or `docker compose` is unavailable in your shell, run commands in the environment where Docker Desktop/Engine is installed and integrated (for example, WSL with Docker integration enabled).
- If Docker reports missing environment variables, ensure `.env` exists in the repository root (you can initialize it with `cp .env.example .env`).
- If Docker services fail due to port conflicts (`3306`, `6379`, `5672`, `15672`), stop conflicting local processes/containers or adjust mapped ports in `docker-compose.yml`.
- If you see missing internal SNAPSHOT dependencies (for example `merchantops-common`, `merchantops-domain`, or `merchantops-infra`), install from root with `-am`: `./mvnw -pl merchantops-api -am -DskipTests install`
- `No plugin found for prefix 'spring-boot'` means the command is being resolved from the aggregator root POM. Run using the module POM: `./mvnw -f merchantops-api/pom.xml spring-boot:run`
- If startup fails with `Port 8080 was already in use`, stop the process using port `8080` or run on another port: `./mvnw -f merchantops-api/pom.xml spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`

## Current Status

- Initial repository setup is complete
- Multi-module backend skeleton is running
