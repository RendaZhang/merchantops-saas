# MerchantOps SaaS

MerchantOps SaaS is a multi-tenant backend project for merchant operations scenarios. The current repository focuses on a working Spring Boot skeleton with JWT authentication, RBAC demo endpoints, tenant/user context propagation, tenant-scoped user-management and ticket-workflow flows, a minimal tenant-scoped audit-event query backbone, a minimal approval flow and approval queue for user disable requests, and a narrow async `USER_CSV` import path with error reporting and failed-row replay on top of the import-job surface, plus Flyway migrations, health checks, request tracing, and OpenAPI support.

## Target Users

- Cross-border e-commerce seller teams
- Platform operators
- Merchant administrators
- Internal support and customer service teams

## Core Goals

- Build a realistic multi-tenant SaaS backend
- Support interview storytelling and portfolio presentation
- Evolve from a portfolio-quality system into an open-source reference project, then evaluate commercial collaboration paths
- Keep the repository easy to run and easy to extend

## What It Does

- Multi-module Spring Boot backend
- JWT login and Bearer-token authentication
- Current-user and current-context endpoints
- Current-tenant user-management read and write flows protected by `USER_READ` / `USER_WRITE`
- Current-tenant ticket workflow read and write flows protected by `TICKET_READ` / `TICKET_WRITE`
- Current-tenant audit-event query backbone protected by `USER_READ`
- Minimal approval flow for `USER_STATUS_DISABLE` with request, queue, review, and execution endpoints
- Current-tenant async import-job create/list/detail/error flows plus failed-row replay, with the narrow `USER_CSV` row-execution path now landed
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

- [docs/README.md](docs/README.md): documentation index
- [docs/project-status.md](docs/project-status.md): implemented scope, current phase, completion status, and known gaps
- [docs/roadmap.md](docs/roadmap.md): next-phase work aligned to the 10-week project plan
- [docs/project-plan.md](docs/project-plan.md): 10-week market-aligned plan for a workflow-first, AI-enhanced vertical SaaS
- [docs/getting-started/README.md](docs/getting-started/README.md): setup and startup guides
- [docs/contributing/README.md](docs/contributing/README.md): contributor and agent workflow guidance
- [docs/reference/README.md](docs/reference/README.md): technical reference index
- [docs/runbooks/README.md](docs/runbooks/README.md): smoke tests, regression sign-off, and verification runbooks
- [docs/architecture/README.md](docs/architecture/README.md): ADRs and architecture notes
- [api-demo.http](api-demo.http): IDE-friendly API request examples

## Summary

- Current project status: [docs/project-status.md](docs/project-status.md)
- Planned next-phase work: [docs/roadmap.md](docs/roadmap.md)
- Current tagged milestone: `v0.1.3` on 2026-03-12 for the completed Week 4 audit and approval baseline
- Project direction: portfolio first, open-source second, and potential commercial exploration after the workflow and AI layers are credible
- Week 2 tenant user-management loop is complete, Week 3 ticket workflow core loop is complete, Week 4 audit/approval governance baseline is complete, and Week 5 async import is active with `USER_CSV` business-row import, error reporting, and failed-row replay now landed
