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
- Profiles & Local Config
- Database Migrations (Flyway)
- API Response & Error Handling
- Authentication (Current Scope)
- Request Tracing (X-Request-Id)
- API Docs & Security
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
├── .env.example
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
- Frameworks: Spring Boot 3.3.8, Spring Web, Spring Validation, Spring Boot Actuator, Spring Security, SpringDoc OpenAPI, Flyway
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

## Profiles & Local Config

- `application.yml` defaults to the `dev` profile and can be overridden:
  - default: `SPRING_PROFILES_ACTIVE=dev`
  - override example: `SPRING_PROFILES_ACTIVE=prod`
- `application-dev.yml` contains local integration settings for:
  - MySQL (`localhost:3306`)
  - Redis (`localhost:6379`)
  - RabbitMQ (`localhost:5672`)
- `application-dev.yml` supports environment-variable overrides (for example `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `RABBITMQ_DEFAULT_USER`, `RABBITMQ_DEFAULT_PASS`), with local-safe defaults.

## Database Migrations (Flyway)

- Flyway dependencies are managed in `merchantops-infra` (`flyway-core`, `flyway-mysql`).
- Migration scripts are loaded from:
  - `merchantops-api/src/main/resources/db/migration`
- Naming convention:
  - `V{version}__{description}.sql` (example: `V1__init_schema.sql`)
- In `dev` profile, Flyway is enabled and runs automatically on application startup.
- Current migrations:
  - `V1__init_schema.sql`: creates base RBAC and tenant tables
  - `V2__seed_demo_data.sql`: inserts first demo tenant/admin/role/permissions data
- A helper tool is available to generate BCrypt hashes for seed/demo users:
  - `merchantops-api/src/main/java/com/renda/merchantops/api/tools/PasswordHashGenerator.java`
- Current demo seed account (local only): `admin` with temporary password `123456`
- Verify migration history in MySQL:

```sql
SELECT installed_rank, version, description, script, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Verify seeded demo data (example):

```sql
SELECT
  (SELECT COUNT(*) FROM tenant) AS tenant_cnt,
  (SELECT COUNT(*) FROM role) AS role_cnt,
  (SELECT COUNT(*) FROM permission) AS perm_cnt,
  (SELECT COUNT(*) FROM users) AS user_cnt,
  (SELECT COUNT(*) FROM user_role) AS user_role_cnt,
  (SELECT COUNT(*) FROM role_permission) AS role_perm_cnt;
```

## API Response & Error Handling

- Unified response wrapper: `ApiResponse<T>` with fields:
  - `code` (business-level result code)
  - `message` (human-readable message)
  - `data` (payload, nullable)
- Shared error model:
  - `ErrorCode` enum in `merchantops-common`
  - `BizException` for business-layer errors
- Global exception mapping:
  - `GlobalExceptionHandler` in `merchantops-api`
  - Handles validation, bind, malformed JSON body, business, and fallback exceptions
  - Maps business error codes to proper HTTP status (for example `NOT_FOUND -> 404`, `FORBIDDEN -> 403`)
  - Returns generic message for unexpected server exceptions (no internal detail leak)
- Example success response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "message": "hello"
  }
}
```

- Example validation failure response:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "message: message must not be blank",
  "data": null
}
```

- Example malformed JSON response:

```json
{
  "code": "BAD_REQUEST",
  "message": "invalid request body",
  "data": null
}
```

## Authentication (Current Scope)

- Implemented endpoint:
  - `POST /api/v1/auth/login`
- Current behavior:
  - Verifies `tenantCode`, `username`, and `password`
  - Requires tenant and user status to be `ACTIVE`
  - Returns JWT `accessToken` on successful login
- Authenticated user endpoint:
  - `GET /api/v1/user/me`
  - Returns current user identity/tenant/roles/permissions from `SecurityContext`
- Token usage:
  - Send `Authorization: Bearer <accessToken>` on protected endpoints
- Error behavior:
  - Missing/invalid token on protected endpoint returns `401 UNAUTHORIZED`
  - Access denied returns `403 FORBIDDEN` via JSON response handler
- Request example:

```json
{
  "tenantCode": "demo-tenant",
  "username": "admin",
  "password": "123456"
}
```

- Response `data` example:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9....",
  "tokenType": "Bearer",
  "expiresIn": 7200
}
```

- JWT config keys (`application-dev.yml`):
  - `jwt.secret`
  - `jwt.expire-seconds`
- Environment variable overrides:
  - `JWT_SECRET`
  - `JWT_EXPIRE_SECONDS`
- Tenant isolation hardening:
  - Role and permission lookup in login flow is constrained by both `userId` and `tenantId` to avoid cross-tenant claim pollution when inconsistent link data exists.

## Request Tracing (X-Request-Id)

- Request header pass-through:
  - If client sends `X-Request-Id`, the same value is used.
- Automatic generation:
  - If client does not send `X-Request-Id`, backend generates a UUID.
- Response header echo:
  - API always returns `X-Request-Id` in response headers.
- Logging correlation:
  - `requestId` is written into MDC and included in console logs.
  - Filter order is fixed to ensure request ID is available when request-completion logs are emitted.

Quick check examples:

```bash
curl -i http://localhost:8080/health
curl -i -H "X-Request-Id: demo-request-id-001" http://localhost:8080/health
```

## API Docs & Security

- OpenAPI endpoint: `GET /v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Swagger security scheme:
  - `bearerAuth` (HTTP Bearer JWT)
- Public endpoints (no auth):
  - `/health`
  - `/actuator/**`
  - `/swagger-ui/**`
  - `/swagger-ui.html`
  - `/v3/api-docs/**`
  - `/api/v1/dev/**`
  - `/api/v1/auth/login`
- Other endpoints require Bearer token authentication.

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

After startup, open Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

Start with an explicit profile if needed:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw -f merchantops-api/pom.xml spring-boot:run
```

Windows Command Prompt / PowerShell equivalents:

```powershell
mvnw.cmd -pl merchantops-api -am -DskipTests install
mvnw.cmd -f merchantops-api/pom.xml spring-boot:run
```

PowerShell profile override example:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
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
- If Flyway reports `Migration checksum mismatch`, do not edit an already-applied `Vx__...sql` migration. Revert that file and create a new migration version (for example `V3__...sql`) for follow-up changes.
- If you see missing internal SNAPSHOT dependencies (for example `merchantops-common`, `merchantops-domain`, or `merchantops-infra`), install from root with `-am`: `./mvnw -pl merchantops-api -am -DskipTests install`
- `No plugin found for prefix 'spring-boot'` means the command is being resolved from the aggregator root POM. Run using the module POM: `./mvnw -f merchantops-api/pom.xml spring-boot:run`
- If startup fails with `Port 8080 was already in use`, stop the process using port `8080` or run on another port: `./mvnw -f merchantops-api/pom.xml spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`
- If Swagger UI does not show newly added endpoints, verify you are connected to the newly started process (not an older process still occupying `8080`), and check `http://localhost:8080/v3/api-docs` directly.

## Current Status

- Initial repository setup is complete
- Multi-module backend skeleton is running
