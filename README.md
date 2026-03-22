# MerchantOps SaaS

`v0.3.0-beta` | workflow-first, AI-enhanced vertical SaaS backend

MerchantOps SaaS is an open-source, multi-tenant backend reference implementation for merchant operations workflows. It combines tenant isolation, JWT and RBAC security, ticket workflow execution, audit and approval patterns, and async import/data-operations flows in a modular Spring Boot codebase.

## Status

- Current tagged milestone: `v0.3.0-beta`
- Milestone meaning: `Week 6 complete: AI Copilot for Ticket Operations beta baseline`
- Current active phase: Week 7 AI Copilot for Import and Data Quality
- Release maturity: beta preview, not production-ready
- Prior baseline: `v0.2.0-alpha` for the completed Week 5 async import and data operations preview

## Current Capabilities

- Tenant-scoped user management with list, detail, create, update, status, role lookup, and role assignment flows
- Tenant-scoped ticket workflow with list, detail, create, assignee change, status change, comment flow, queue filters, narrowed AI interaction-history reads with runtime usage/cost metadata, and suggestion-only AI summary, triage, and internal reply-draft generation paths
- Audit-event query backbone plus minimal approval flow for `USER_STATUS_DISABLE`
- Async import jobs with create, list, detail, paged error reporting, failed-row replay, whole-file replay, selective replay, edited replay, queued-job recovery, and stale-processing safeguards
- JWT authentication, request tracing, Flyway migrations, health checks, and OpenAPI/Swagger support

## Known Limits

- The public import surface currently supports one business import type only: `USER_CSV`
- There is no frontend or tenant admin UI in this repository
- The current public AI surface is limited to one narrowed ticket AI interaction-history read endpoint with runtime usage/cost metadata plus three suggestion-only ticket generation endpoints: summary, triage, and internal reply draft; broader ticket AI flows and any AI write-back remain pending
- This release line is intended for evaluation and contribution, not production deployment

## Quick Start

Requirements:

- JDK 21
- Maven Wrapper or Maven 3.9.x
- Docker Compose
- Access to Maven Central

Start the local environment:

```powershell
Copy-Item .env.example .env
docker compose up -d
.\mvnw.cmd -pl merchantops-api -am -DskipTests install
.\mvnw.cmd -f merchantops-api/pom.xml spring-boot:run
```

The API also auto-loads the repository-root local `.env` during dev-profile `spring-boot:run`, so local AI provider keys should stay there instead of in tracked files.

After startup:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Health: `http://localhost:8080/health`

For the full setup flow, see [docs/getting-started/README.md](docs/getting-started/README.md).

## Documentation

- [docs/README.md](docs/README.md): documentation index
- [docs/project-status.md](docs/project-status.md): current implementation reality, tagged baseline, and known gaps
- [docs/roadmap.md](docs/roadmap.md): active phase and near-term next steps
- [docs/project-plan.md](docs/project-plan.md): 10-week workflow-first, AI-enhanced project plan
- [docs/reference/README.md](docs/reference/README.md): technical reference index
- [docs/runbooks/README.md](docs/runbooks/README.md): verification runbooks and regression guidance
- [docs/architecture/README.md](docs/architecture/README.md): ADRs and architecture notes
- [api-demo.http](api-demo.http): IDE-friendly API request examples

## Project Direction

- Portfolio-quality system first
- Open-source reference implementation second
- Commercial exploration only after the workflow and AI layers become credible

## Contributing, Security, and License

- [CONTRIBUTING.md](CONTRIBUTING.md)
- [SECURITY.md](SECURITY.md)
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
- [LICENSE](LICENSE)
