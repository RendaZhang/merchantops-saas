# MerchantOps SaaS

MerchantOps SaaS is a multi-tenant backend project for merchant operations scenarios. The current repository focuses on a working Spring Boot skeleton with JWT authentication, RBAC demo endpoints, tenant/user context propagation, tenant-scoped user queries, Flyway migrations, health checks, request tracing, and OpenAPI support.

## Target Users

- Cross-border e-commerce seller teams
- Platform operators
- Merchant administrators
- Internal support and customer service teams

## Core Goals

- Build a realistic multi-tenant SaaS backend
- Support interview storytelling and portfolio presentation
- Keep the repository easy to run and easy to extend

## What It Does

- Multi-module Spring Boot backend
- JWT login and Bearer-token authentication
- Current-user and current-context endpoints
- Current-tenant user listing protected by `USER_READ`
- RBAC demo endpoints and permission interception
- Flyway migrations, enriched Swagger / OpenAPI docs, health checks, and request tracing

## Repository Structure

```text
merchantops-saas/
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
└── pom.xml
```

## Module Responsibilities

- `merchantops-api`: REST controllers, DTOs, security, and application bootstrap
- `merchantops-domain`: business models, domain services, and rules
- `merchantops-infra`: persistence, repositories, and infrastructure integration
- `merchantops-common`: shared responses, exceptions, and utilities

## Requirements

- JDK 21
- Maven Wrapper or Maven 3.9.x
- Docker Compose for the default local environment
- Access to Maven Central

## Quick Start

1. Create the local environment file and start dependencies:

```bash
cp .env.example .env
docker compose up -d
```

PowerShell:

```powershell
Copy-Item .env.example .env
docker compose up -d
```

2. Build the API module and its dependencies:

```bash
./mvnw -pl merchantops-api -am -DskipTests install
```

PowerShell:

```powershell
mvnw.cmd -pl merchantops-api -am -DskipTests install
```

3. Start the API:

```bash
./mvnw -f merchantops-api/pom.xml spring-boot:run
```

PowerShell:

```powershell
mvnw.cmd -f merchantops-api/pom.xml spring-boot:run
```

After startup:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Health: `http://localhost:8080/health`

More setup details live in [docs/getting-started/README.md](docs/getting-started/README.md).

## Documentation

- [docs/project-plan.md](docs/project-plan.md): 5-week delivery plan from platform foundation to business modules and engineering hardening
- [docs/README.md](docs/README.md): documentation index
- [docs/project-status.md](docs/project-status.md): implemented scope, current phase, completion status, and known gaps
- [docs/roadmap.md](docs/roadmap.md): next-phase work aligned to the 5-week project plan
- [docs/architecture/README.md](docs/architecture/README.md): ADRs and architecture notes
- [docs/getting-started/local-environment.md](docs/getting-started/local-environment.md): Docker services, ports, and `.env`
- [docs/getting-started/quick-start.md](docs/getting-started/quick-start.md): full local startup flow
- [docs/reference/configuration.md](docs/reference/configuration.md): profiles and environment variables
- [docs/reference/database-migrations.md](docs/reference/database-migrations.md): Flyway and seed data details
- [docs/reference/api-conventions.md](docs/reference/api-conventions.md): response and error handling rules
- [docs/reference/authentication-and-rbac.md](docs/reference/authentication-and-rbac.md): login, JWT, context, and RBAC behavior
- [docs/reference/user-management.md](docs/reference/user-management.md): current tenant-scoped user API and the current Week 2 delivery boundary
- [docs/reference/api-docs.md](docs/reference/api-docs.md): Swagger and OpenAPI access
- [docs/reference/observability.md](docs/reference/observability.md): health checks and request tracing
- [docs/runbooks/local-smoke-test.md](docs/runbooks/local-smoke-test.md): manual verification flow
- [docs/runbooks/regression-checklist.md](docs/runbooks/regression-checklist.md): baseline regression checks for foundation and current user-management API
- [docs/runbooks/troubleshooting.md](docs/runbooks/troubleshooting.md): common local problems
- [docs/architecture/tenant-rbac-integrity-gap.md](docs/architecture/tenant-rbac-integrity-gap.md): recorded database isolation gap
- [api-demo.http](api-demo.http): IDE-friendly API request examples

## Summary

- Current project status: [docs/project-status.md](docs/project-status.md)
- Planned next-phase work: [docs/roadmap.md](docs/roadmap.md)
- Week 1 foundation is complete and Week 2 first business loop (tenant user management) is underway
