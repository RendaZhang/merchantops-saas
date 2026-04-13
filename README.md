# MerchantOps SaaS

`v0.7.0-beta` | workflow-first, AI-enhanced backend reference implementation

MerchantOps SaaS is an open-source, multi-tenant backend reference implementation for merchant operations workflows. It shows how tenant isolation, JWT/RBAC security, ticket execution, async import operations, audit/approval patterns, AI suggestions, feature flags, Docker delivery, and CI can fit together in a modular Spring Boot system.

This repository is built for portfolio review, open-source handoff, and implementation study. It is a beta reference implementation, not a production-ready SaaS platform.

## Current Milestone

- Current tagged milestone: `v0.7.0-beta`
- Tagged baseline meaning: Week 10 complete - delivery hardening and portfolio packaging beta baseline
- Current phase state: Week 10 complete; Productization Baseline planning next
- Week 10 completed baseline: Slice A feature flags, Slice B Dockerized API, Slice C minimal GitHub Actions CI, and Slice D portfolio/open-source handoff packaging
- Previous tagged milestone: `v0.6.0-beta` for the completed Week 9 AI Governance, Eval, Cost, and Usage beta baseline

## Core Capabilities

- Tenant-scoped authentication, context propagation, user management, role assignment, and stale-token revalidation.
- Ticket workflow with list/detail/create, assignment, status transitions, comments, workflow logs, and tenant-scoped audit events.
- Async `USER_CSV` import jobs with queued execution, paged errors, replay variants, derived-job lineage, and stale-processing recovery.
- Human-reviewed approval flows for user disable, import selective replay proposals, and ticket comment proposals.
- AI-assisted ticket and import workflows where public AI endpoints remain read-only or suggestion-only.
- AI governance reads for narrowed interaction history and tenant-scoped aggregate usage/cost metadata, including prompt-version breakdowns.
- Fixed tenant-scoped feature flags for six AI generation endpoints and two approval-backed workflow bridges.
- Local Maven startup, Dockerized API startup, OpenAPI/Swagger docs, and a minimal no-secret GitHub Actions quality gate.

## Quick Start

Requirements:

- JDK 21
- Docker Desktop or another Docker Engine runtime with `docker compose`
- Access to Maven Central

Prepare local infrastructure:

```powershell
Copy-Item .env.example .env
docker compose up -d
```

Choose one API startup path.

Local Maven:

```powershell
.\mvnw.cmd -pl merchantops-api -am -DskipTests install
.\mvnw.cmd -f merchantops-api/pom.xml spring-boot:run
```

Dockerized API:

```powershell
docker build -t merchantops-api:local .
docker run --rm --name merchantops-api-local `
  --env-file .env `
  --network merchantops-infra `
  -p 8080:8080 `
  -e MYSQL_HOST=mysql `
  -e REDIS_HOST=redis `
  -e RABBITMQ_HOST=rabbitmq `
  merchantops-api:local
```

After startup:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/health`
- Actuator health: `http://localhost:8080/actuator/health`

For the full setup flow, use [Getting Started](docs/getting-started/README.md). For a 5-10 minute review path, use the [Project Showcase](docs/getting-started/project-showcase.md).

## CI And Verification

Default local regression:

```powershell
.\mvnw.cmd -pl merchantops-api -am test
```

GitHub Actions runs the Linux equivalent on pull requests and `main` pushes, then verifies that the API image builds with:

```bash
docker build -t merchantops-api:ci .
```

The CI gate is intentionally minimal. It does not deploy, publish Docker images, start the Dockerized API for live smoke, run live AI provider checks, or run the opt-in real MySQL migration suite. Current CI evidence and exact verification boundaries are tracked in [Project Status](docs/project-status.md) and [Automated Tests](docs/runbooks/automated-tests.md).

## Docs Map

- [Documentation index](docs/README.md): reading order and high-value jump targets
- [Getting Started](docs/getting-started/README.md): local environment, quick start, and project showcase
- [Project Showcase](docs/getting-started/project-showcase.md): short demo and handoff path
- [Project Status](docs/project-status.md): current implemented reality, CI evidence, and known gaps
- [Roadmap](docs/roadmap.md): active release-line milestone, active slice, candidate next slices, and stop condition
- [Product Strategy](docs/product-strategy.md): long-term direction from the `v0.7.0-beta` foundation baseline
- [Project Plan](docs/project-plan.md): planning entry point and link to the archived foundation plan
- [Completed 10-Week Foundation Plan](docs/archive/completed-10-week-foundation-plan.md): historical Week 1-10 build plan
- [Reference](docs/reference/README.md): public contracts and technical reference pages
- [Runbooks](docs/runbooks/README.md): smoke tests, regression sign-off, and verification guidance
- [Architecture](docs/architecture/README.md): ADRs, diagrams, and structural notes
- [api-demo.http](api-demo.http): IDE-friendly request examples

## Current Limitations

- There is no frontend, tenant admin UI, production deployment automation, or Docker image publishing in this repository.
- The public import workflow currently supports one business import type: `USER_CSV`.
- Feature flags are fixed-key and tenant-scoped only; there is no cross-tenant admin surface, percentage rollout, or generic flag platform.
- Public AI endpoints are read-only or suggestion-only. AI-generated ticket comments and import selective replay still go through separate human-reviewed approval bridges before execution.
- AI usage/cost metadata is a governance read surface over stored runtime metadata, not billing, ledger, invoice, or commercial metering infrastructure.
- CI does not require AI provider secrets and does not prove live vendor behavior.
- K8s, Helm, production secret-management guidance, performance artifacts, refresh tokens, logout, token revocation, and a broader reporting system remain outside the current baseline.

## Contributing, Security, And License

- [CONTRIBUTING.md](CONTRIBUTING.md)
- [SECURITY.md](SECURITY.md)
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
- [LICENSE](LICENSE)
