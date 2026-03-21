# MerchantOps SaaS

`v0.2.0-alpha` | workflow-first, AI-enhanced vertical SaaS backend

MerchantOps SaaS is an open-source, multi-tenant backend reference implementation for merchant operations workflows. It combines tenant isolation, JWT and RBAC security, ticket workflow execution, audit and approval patterns, and async import/data-operations flows in a modular Spring Boot codebase.

## Status

- Current tagged milestone: `v0.2.0-alpha`
- Milestone meaning: `Week 5 complete: async import and data operations preview`
- Next active phase: Week 6 AI Copilot for Ticket Operations
- Release maturity: alpha preview, not production-ready
- Prior baseline: `v0.1.3` for the completed Week 4 audit and approval baseline

## Current Capabilities

- Tenant-scoped user management with list, detail, create, update, status, role lookup, and role assignment flows
- Tenant-scoped ticket workflow with list, detail, create, assignee change, status change, comment flow, queue filters, and suggestion-only AI summary and triage read paths
- Audit-event query backbone plus minimal approval flow for `USER_STATUS_DISABLE`
- Async import jobs with create, list, detail, paged error reporting, failed-row replay, whole-file replay, selective replay, edited replay, queued-job recovery, and stale-processing safeguards
- JWT authentication, request tracing, Flyway migrations, health checks, and OpenAPI/Swagger support

## Known Limits

- The public import surface currently supports one business import type only: `USER_CSV`
- There is no frontend or tenant admin UI in this repository
- The current public AI surface is limited to two suggestion-only ticket endpoints: summary and triage; reply drafts, broader ticket AI flows, and any AI write-back remain pending
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
