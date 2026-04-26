# Project Showcase

Use this page when you need to explain MerchantOps SaaS quickly to a reviewer, interviewer, or new contributor. It is a practical 5-10 minute handoff path, not a full runbook.

## Purpose

MerchantOps SaaS is a workflow-first SaaS reference implementation for merchant operations teams. The project is meant to demonstrate how a realistic Spring Boot backend plus a minimal admin-console entry can combine tenant isolation, RBAC, system-of-action workflows, async imports, audit/approval governance, AI-assisted operations, rollout control, Docker delivery, and CI without presenting unfinished infrastructure as production-ready.

The short story:

- it is not a CRUD-only demo
- the first product-facing admin console now proves login, tenant context, token restoration, sign-out, a read-only Tickets workflow screen, and a Feature Flags control screen
- AI is embedded into ticket and import workflows instead of being a standalone chat surface
- AI outputs stay read-only or suggestion-only unless a separate human-reviewed workflow bridge executes them
- governance metadata is visible enough for operational review without becoming billing or ledger infrastructure
- delivery hardening exists through local Docker startup and minimal CI, not through deployment automation

## What To Demo First

Start with the path that proves the project is runnable and workflow-centered:

1. Open the admin console at `http://localhost:8081` for the production-like runtime, or `http://localhost:5173` for local Vite development.
2. Login as the seeded demo admin for tenant `demo-shop`.
3. Show the dashboard tenant/operator context, then open the read-only Tickets queue and Feature Flags control screen.
4. Open Swagger UI at `http://localhost:8080/swagger-ui/index.html` for the deeper API workflow demo.
5. Use Swagger or `api-demo.http` for deeper ticket workflow operations before showing AI.
6. Show AI suggestion endpoints and then the separate approval-backed execution bridge.
7. Show import jobs and import AI reads.
8. Close with feature flags, usage summary, Docker startup, frontend verification, and CI evidence.

Use [quick-start.md](quick-start.md) for API startup commands, [admin-console.md](admin-console.md) for the frontend run path, and [../../api-demo.http](../../api-demo.http) for request examples.

## Suggested Demo Flow

### 1. Login And Tenant Context

- Use `POST /api/v1/auth/login` with tenant `demo-shop`, username `admin`, and password `123456`.
- Use the returned JWT against `GET /api/v1/context`.
- Explain that protected reads and writes revalidate tenant status, user status, roles, and permissions instead of trusting stale claims indefinitely.

### 2. Ticket Workflow

- Use `GET /api/v1/tickets` to show the tenant-scoped queue.
- Open a ticket with `GET /api/v1/tickets/{id}`.
- If demonstrating writes, use the ticket create, assignment, status, and comment endpoints from Swagger or `api-demo.http`.
- Call out that workflow logs and audit events are separate: ticket logs explain the business timeline, while audit rows support governance.

### 3. Ticket AI Suggestions

- Use the ticket AI endpoints:
  - `GET /api/v1/tickets/{id}/ai-interactions`
  - `POST /api/v1/tickets/{id}/ai-summary`
  - `POST /api/v1/tickets/{id}/ai-triage`
  - `POST /api/v1/tickets/{id}/ai-reply-draft`
- Explain that these endpoints do not change ticket status, create comments, trigger approvals, or send external messages.
- If no live AI provider is configured, use the documented degraded-mode behavior and stored interaction records as the governance story; do not present CI as live-provider validation.

### 4. Approval-Backed Ticket Comment Proposal

- Use `POST /api/v1/tickets/{id}/comments/proposals/ai-reply-draft` to show how an AI reply draft can become a human-reviewed proposal.
- Review the proposal through the approval request endpoints.
- Explain the three-step pattern: suggestion -> approval request -> human-reviewed execution.
- Call out duplicate-pending suppression, self-approval rejection, and no execution on reject as the safety boundary.

### 5. Import Jobs And Import AI

- Use `POST /api/v1/import-jobs` with a `USER_CSV` file to create an async job.
- Use `GET /api/v1/import-jobs`, `GET /api/v1/import-jobs/{id}`, and `GET /api/v1/import-jobs/{id}/errors` to show progress and row-level failure reporting.
- Use the import AI endpoints:
  - `GET /api/v1/import-jobs/{id}/ai-interactions`
  - `POST /api/v1/import-jobs/{id}/ai-error-summary`
  - `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`
  - `POST /api/v1/import-jobs/{id}/ai-fix-recommendation`
- Explain that import AI uses bounded, sanitized context and does not forward raw CSV payload values such as passwords to the provider.

### 6. Approval-Backed Import Replay Proposal

- Use `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals` when a failed `USER_CSV` job has replayable error codes.
- Explain that the proposal stores a narrow safe payload and only dispatches into the existing selective replay execution path after approval.
- Avoid describing this as direct AI write-back; it is a separate approval-governed workflow bridge.

### 7. Feature Flags

- Use `GET /api/v1/feature-flags` to show the fixed tenant-scoped flag set.
- Show that the six AI generation endpoints and two workflow bridges are gated by separate persisted flags:
  - `ai.ticket.summary.enabled`
  - `ai.ticket.triage.enabled`
  - `ai.ticket.reply-draft.enabled`
  - `ai.import.error-summary.enabled`
  - `ai.import.mapping-suggestion.enabled`
  - `ai.import.fix-recommendation.enabled`
  - `workflow.import.selective-replay-proposal.enabled`
  - `workflow.ticket.comment-proposal.enabled`
- Explain that this is rollout control, not a generic flag platform or billing policy engine.

### 8. Usage Summary

- Use `GET /api/v1/ai-interactions/usage-summary`.
- Show totals plus `byInteractionType`, `byStatus`, and `byPromptVersion` breakdowns.
- Explain that this is aggregate governance visibility over stored runtime metadata. It intentionally excludes raw prompts, raw provider payloads, per-request cross-entity detail lists, billing, ledger, and invoice semantics.

### 9. Docker And CI Proof

- Show the local startup paths:
  - Maven: `.\mvnw.cmd -f merchantops-api/pom.xml spring-boot:run`
  - Dockerized API: `docker build -t merchantops-api:local .` plus the documented `docker run --env-file .env --network merchantops-infra ...` command
  - Same-origin admin + API runtime: `docker compose -f docker-compose.yml -f docker-compose.runtime.yml up -d --build`
- Show that GitHub Actions currently runs three no-secret jobs for pull requests and `main` pushes:
  - `Maven Test`
  - `Admin Web Check`
  - `Docker Build`
- Keep the boundary clear: CI proves default Maven regression, admin frontend checks, and image construction, not deployment, image publishing, Dockerized live smoke, live AI provider behavior, or the opt-in real MySQL migration suite.

## What To Say About Architecture

- The codebase is a modular Spring Boot backend with MySQL, Redis, RabbitMQ, Flyway, JWT security, request tracing, and OpenAPI/Swagger support.
- The admin console is a standalone Vite + React frontend at `merchantops-admin-web/`; it is not served from Spring Boot static resources.
- Tenant scope is explicit across user, ticket, approval, import, feature-flag, and AI read paths.
- The workflow model separates system-of-record data, system-of-action transitions, audit events, approval requests, and AI interaction records.
- AI is intentionally bounded: suggestion endpoints store governance metadata, and execution happens only through separate approval-backed workflow bridges.
- Docker support includes a reproducible API image and an Nginx-served admin image that proxies same-origin `/api` traffic to the API container on the compose-managed infra network.

## Intentionally Out Of Scope

- Full workflow data screens beyond the current Tickets and Feature Flags admin screens
- Production deployment automation
- Docker image publishing
- K8s or Helm manifests
- Live AI provider checks in CI
- Direct AI write-back from public AI endpoints
- Tenant BYOK, streaming, tool calling, model routing, RAG, MCP, or Spring AI migration expansion
- Billing, ledger, invoice, or commercial metering infrastructure
- Generic feature-flag platform with cross-tenant administration or percentage rollout

## Verification Checklist

Use this short checklist for a handoff review:

- Local infra starts with `docker compose up -d`.
- One API startup path works: local Maven or Dockerized API.
- The admin console starts either with `npm run dev` from `merchantops-admin-web` for local development or at `http://localhost:8081` through `docker-compose.runtime.yml` for production-like runtime.
- Swagger UI and `/health` load on port `8080`.
- Seeded admin login works for `demo-shop` in the admin console.
- The dashboard renders tenant/operator context and restores the local token after refresh.
- Ticket workflow reads and at least one ticket detail path work.
- The Feature Flags screen loads the fixed eight-key inventory and can restore a toggled flag to its original state.
- AI endpoints are described as read-only or suggestion-only, with approval bridges shown separately.
- Import job reads and error reporting are shown before import AI.
- `GET /api/v1/feature-flags` and `GET /api/v1/ai-interactions/usage-summary` are included in the governance story.
- `.\mvnw.cmd -pl merchantops-api -am test` is the default local regression.
- `npm run typecheck`, `npm run lint`, and `npm run build` are the frontend workspace checks.
- Current remote CI evidence is checked in [../project-status.md](../project-status.md).
