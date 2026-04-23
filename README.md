# MerchantOps SaaS

`v0.7.0-beta` | workflow-first, AI-enhanced SaaS reference implementation

MerchantOps SaaS is an open-source, multi-tenant SaaS reference implementation for merchant operations workflows. It shows how tenant isolation, JWT/RBAC security, ticket execution, async import operations, audit/approval patterns, AI suggestions, feature flags, Docker delivery, minimal admin-console UX, and CI can fit together around a modular Spring Boot backend.

This repository is built for portfolio review, open-source handoff, and implementation study. It is a beta reference implementation, not a production-ready SaaS platform.

## Current Milestone

- Current tagged milestone: `v0.7.0-beta`
- Tagged baseline meaning: Week 10 complete - delivery hardening and portfolio packaging beta baseline
- Current phase state: Productization Baseline active; Slice A admin entry, Slice B auth-session/logout, Slice C same-origin runtime, Slice D user-role tenant-integrity hardening, Slice E first read-only Tickets screen, Slice F root ticket actor tenant-integrity hardening, Slice G-A auth-session cleanup scheduler, and Slice G-B1 logout-all sessions contract are complete; Slice H Next Admin Workflow Screen is next
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
- Minimal Vite/React admin console for login, current tenant context, token restoration, backend sign-out, a read-only Tickets queue, workflow navigation placeholders, and same-origin Nginx runtime packaging.
- Local Maven startup, Dockerized API startup, production-like admin + API runtime compose, OpenAPI/Swagger docs, and a minimal no-secret GitHub Actions quality gate.

## Quick Start

Requirements:

- JDK 21
- Docker Desktop or another Docker Engine runtime with `docker compose`
- Node.js and npm for the admin console
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

Run the minimal admin console in a second terminal:

```powershell
cd merchantops-admin-web
npm install
npm run dev
```

Open `http://localhost:5173` and log in with tenant `demo-shop`, username `admin`, and password `123456`.

Production-like admin + API runtime:

```powershell
docker compose -f docker-compose.yml -f docker-compose.runtime.yml up -d --build
```

Open `http://localhost:8081` for the Nginx-served admin console with same-origin `/api` proxying.

For the full setup flow, use [Getting Started](docs/getting-started/README.md). For the frontend path, use [Admin Console](docs/getting-started/admin-console.md). For a 5-10 minute review path, use the [Project Showcase](docs/getting-started/project-showcase.md).

## CI And Verification

Default local regression:

```powershell
.\mvnw.cmd -pl merchantops-api -am test
```

Frontend workspace checks:

```powershell
cd merchantops-admin-web
npm run typecheck
npm run lint
npm run build
```

GitHub Actions runs the Linux equivalent on pull requests and `main` pushes, runs the admin web typecheck/lint/build, then verifies that both runtime images build with:

```bash
docker build -t merchantops-api:ci .
docker build -t merchantops-admin-web:ci ./merchantops-admin-web
```

The CI gate is intentionally minimal. It does not deploy, publish Docker images, start the Dockerized runtime stack for live smoke, run live AI provider checks, or run the opt-in real MySQL migration suite. Current CI evidence and exact verification boundaries are tracked in [Project Status](docs/project-status.md) and [Automated Tests](docs/runbooks/automated-tests.md).

## Docs Map

- [Documentation index](docs/README.md): reading order and high-value jump targets
- [Getting Started](docs/getting-started/README.md): local environment, quick start, admin console, and project showcase
- [Admin Console](docs/getting-started/admin-console.md): minimal frontend run path and smoke test
- [Project Showcase](docs/getting-started/project-showcase.md): short demo and handoff path
- [Project Status](docs/project-status.md): current implemented reality, CI evidence, and known gaps
- [Roadmap](docs/roadmap.md): active release-line milestone, active slice, candidate next slices, and stop condition
- [Product Strategy](docs/product-strategy.md): long-term direction from the `v0.7.0-beta` foundation baseline
- [Project Plan](docs/project-plan.md): planning entry point and link to the archived foundation plan
- [Completed 10-Week Foundation Plan](docs/archive/completed-10-week-foundation-plan.md): historical Week 1-10 build plan
- [Reference](docs/reference/README.md): public contracts and technical reference pages
- [Runbooks](docs/runbooks/README.md): smoke tests, regression sign-off, and verification guidance
- [Architecture](docs/architecture/README.md): ADRs, diagrams, admin-console architecture, and structural notes
- [api-demo.http](api-demo.http): IDE-friendly request examples

## Current Limitations

- The admin console currently includes login/context/sign-out plus a read-only Tickets queue; ticket detail, ticket mutations, filters, pagination controls, Approvals, Imports, AI Interactions, and Feature Flags remain placeholders or later workflow screens.
- The public import workflow currently supports one business import type: `USER_CSV`.
- Feature flags are fixed-key and tenant-scoped only; there is no cross-tenant admin surface, percentage rollout, or generic flag platform.
- Public AI endpoints are read-only or suggestion-only. AI-generated ticket comments and import selective replay still go through separate human-reviewed approval bridges before execution.
- AI usage/cost metadata is a governance read surface over stored runtime metadata, not billing, ledger, invoice, or commercial metering infrastructure.
- CI does not require AI provider secrets and does not prove live vendor behavior.
- K8s, Helm, real secret-manager integration, performance artifacts, refresh tokens, cookie/session rotation, device-aware session management, and a broader reporting system remain outside the current baseline.

## Contributing, Security, And License

- [CONTRIBUTING.md](CONTRIBUTING.md)
- [SECURITY.md](SECURITY.md)
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
- [LICENSE](LICENSE)
