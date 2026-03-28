# Project Status

Last updated: 2026-03-28

> Maintenance note: keep this page as the source of truth for current implementation reality, current public baseline, and known gaps. If the active phase, public endpoints, automated coverage, or current limitations change, update this page before mirroring the change into [roadmap.md](roadmap.md) or [project-plan.md](project-plan.md).

## Overview

MerchantOps SaaS now sits on a completed Week 1-6 baseline with Week 7 AI Copilot for Import and Data Quality active. The public surface covers tenant-scoped user management, ticket workflow, audit/approval, import jobs, read-only ticket AI summary, triage, internal reply-draft, interaction-history endpoints, and four read-only import AI endpoints: interaction history, error summary, narrow mapping suggestion, and narrow fix recommendation.

## Current Phase Summary

- Current phase: Week 7 AI Copilot For Import And Data Quality.
- Stable completed tagged baselines: Week 2 tenant user management, Week 3 ticket workflow, Week 4 audit/approval, Week 5 async import and data operations, and Week 6 AI Copilot for Ticket Operations.
- Week 5 remains complete with import submission/list/detail/error reporting, narrowed `USER_CSV` business-row execution, filtered queue reads, failed-row replay variants, whole-file replay for full-failure sources, and derived-job lineage.
- Week 6 is now complete with four public suggestion-only or read-only ticket AI slices exposed as `GET /api/v1/tickets/{id}/ai-interactions`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, and `POST /api/v1/tickets/{id}/ai-reply-draft` under tenant scope, `TICKET_READ`, explicit prompt versioning, controlled provider degradation, dedicated AI interaction persistence, and operator-visible runtime usage/cost metadata on interaction history.
- Week 7 Slice A is now live as `POST /api/v1/import-jobs/{id}/ai-error-summary` under tenant scope and `USER_READ`, with read-only suggestion-only behavior, prompt-time row sanitization, controlled provider degradation, and dedicated `ai_interaction_record` persistence as `entityType=IMPORT_JOB` plus `interactionType=ERROR_SUMMARY`.
- Week 7 Slice B is now live as `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion` under tenant scope and `USER_READ`, reusing the same import AI runtime, sanitized-context rule, and degraded-mode model while staying read-only and suggestion-only.
- Week 7 Slice C is now live as `POST /api/v1/import-jobs/{id}/ai-fix-recommendation` under tenant scope and `USER_READ`, reusing the same import AI runtime while staying read-only, suggestion-only, grounded in local row-level `errorCode` groups, and protected by local sensitive-output rejection.
- Week 7 Slice D is now live as `GET /api/v1/import-jobs/{id}/ai-interactions` under tenant scope and `USER_READ`, reusing existing `ai_interaction_record` storage for narrowed, read-only, operator-visible import AI history with exact-match filters and stable ordering `createdAt DESC, id DESC`.
- The current active phase remains Week 7 AI Copilot For Import And Data Quality, with the next near-term focus now narrowed to stabilizing the full import AI read baseline rather than deciding whether to add public import AI history.
- Exact endpoint contracts live in [reference/README.md](reference/README.md); this page keeps the phase-level truth and current limits.

## Release Baseline

- Current tagged milestone: `v0.3.0-beta` on 2026-03-22, recorded as `Week 6 complete: AI Copilot for Ticket Operations beta baseline`.
- Previous tagged milestone: `v0.2.0-alpha` on 2026-03-19, recorded as `Week 5 complete: async import and data operations preview`.
- Earlier milestones: `v0.1.3` on 2026-03-12 (`Week 4 complete: audit and approval baseline`), `v0.1.2` on 2026-03-11 (`Week 3 complete: ticket workflow baseline`), `v0.1.1` on 2026-03-11 (`Week 2 complete: tenant user management loop`), and `v0.1.0` on 2026-03-09 (`Week 1 complete: foundation phase`).
- The current tagged baseline now includes the full Week 6 public ticket AI surface and its latest runtime/documentation hardening.

## Current Repository Baseline

### Public workflow surface

- Auth and context: `POST /api/v1/auth/login`, `GET /api/v1/user/me`, and `GET /api/v1/context`.
- User management: tenant-scoped list/detail/create/update/status/role-assignment plus role lookup and disable-request initiation.
- Ticket workflow: tenant-scoped list/detail/create/assignee/status/comment flow with queue filters and reopen support.
- AI-assisted ticket read path: `GET /api/v1/tickets/{id}/ai-interactions` for narrowed interaction-history visibility plus operator-visible runtime usage/cost metadata, `POST /api/v1/tickets/{id}/ai-summary` for suggestion-only summaries, `POST /api/v1/tickets/{id}/ai-triage` for suggestion-only classification and priority guidance, and `POST /api/v1/tickets/{id}/ai-reply-draft` for internal comment-style reply drafts from ticket detail context.
- AI-assisted import read path: `GET /api/v1/import-jobs/{id}/ai-interactions` for narrowed stored import AI interaction history with operator-visible runtime usage/cost metadata when present; `POST /api/v1/import-jobs/{id}/ai-error-summary` for a suggestion-only summary built from current-tenant import detail, `errorCodeCounts`, and the first sanitized failed-row window without forwarding raw CSV payload values to the provider; `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion` for a suggestion-only canonical-field mapping proposal built from sanitized header/global parse signal plus the same bounded row-summary context; and `POST /api/v1/import-jobs/{id}/ai-fix-recommendation` for a suggestion-only fix recommendation built from grounded row-level `errorCode` groups without returning replacement values.
- Governance: entity-scoped `GET /api/v1/audit-events` plus minimal approval request queue/detail/approve/reject flow for `USER_STATUS_DISABLE`.
- Import jobs: tenant-scoped create/list/detail/ai-interactions/ai-error-summary/ai-mapping-suggestion/ai-fix-recommendation/replay/replay-file/selective-replay/edited-replay/error-page flow with `USER_CSV` processing, filtered queue reads, quoted CSV record parsing, `errorCodeCounts`, row-level item errors, replay-derived job lineage, scope-only replay audit metadata, and the current Week 5 plus Week 7 Slice A/B/C/D reporting/guidance surface.

### Shared runtime and internal baseline

- Multi-module Spring Boot backend with MySQL, Redis, RabbitMQ, Flyway, request tracing, and unified API response / exception handling.
- Tenant-aware query and command services for users, tickets, approvals, import jobs, and the current ticket interaction-history, ticket-summary, ticket-triage, and ticket reply-draft AI read paths.
- JWT claim revalidation against current tenant status, user status, roles, and permissions on protected requests.
- Instance-level AI provider configuration under `merchantops.ai.*`, repository-root `.env` bootstrap for local dev-profile `spring-boot:run`, explicit summary, triage, reply-draft, and import-error-summary prompt versions, timeout-based degradation, and provider-normalized adapters that support OpenAI Responses plus DeepSeek Chat Completions JSON-output paths for the current ticket and import AI slices.
- Dedicated `ai_interaction_record` persistence for AI runtime metadata, separate from `ticket_operation_log` and generic `audit_event`, with current public rows for both `TICKET` and `IMPORT_JOB` entities.
- Local import file storage abstraction with after-commit queue publish, scheduled queued/stale-processing recovery, worker consumption, stale-processing restart/fail handling, system-generated replay-file writes, and configurable chunk / recovery / row-limit controls.
- Focused automated coverage for auth, user management, ticket workflow, AI interaction history, AI summary, AI triage, AI reply draft, import jobs, audit, and approval behavior.

## Current Public AI Baseline

- The current public AI slices are `GET /api/v1/tickets/{id}/ai-interactions`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, `POST /api/v1/tickets/{id}/ai-reply-draft`, `GET /api/v1/import-jobs/{id}/ai-interactions`, `POST /api/v1/import-jobs/{id}/ai-error-summary`, `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`, and `POST /api/v1/import-jobs/{id}/ai-fix-recommendation`.
- All eight slices are read-only. The six `POST` endpoints remain suggestion-only: they do not change ticket status, write comments, mutate import jobs, mutate import error rows, or trigger approvals or replay flows, and the two `GET` history endpoints only expose narrowed stored metadata.
- Reply-draft semantics are explicitly internal and comment-style: the endpoint returns a structured draft plus `draftText`, but still does not create a comment or send an external message.
- The current AI context is limited to the target current-tenant ticket's core fields plus the most recent comments and workflow logs, with prompt-time truncation and explicit `earlier ... omitted` markers when older history exists.
- The import AI context is limited to current-tenant import detail, `errorCodeCounts`, sanitized header/global parse-error signal, grounded row-level `errorCode` groups, and the first 20 failed rows after local structural sanitization; raw `itemErrors.rawPayload` values, including `USER_CSV.password`, are not forwarded to the provider, and fix recommendation additionally rejects sensitive echoed output before returning a response.
- The interaction-history surfaces return both successful and controlled-failure records with exact-match `interactionType` and `status` filters plus stable `createdAt DESC, id DESC` ordering.
- The interaction-history surfaces now expose ticket-scoped and import-scoped runtime usage/cost metadata when present, return those fields as `null` when unavailable, and still do not expose raw prompt text or raw provider payload.
- The current provider ownership model is instance-level configuration rather than tenant BYOK.
- The completed Week 6 ticket AI baseline and the Week 7 import AI read baseline should now stay stable while automatic write-back remains out of scope.

## Current Limitations

- Import jobs currently support one business import type only: `USER_CSV`.
- The `USER_CSV` schema is fixed to `username,displayName,email,password,roleCodes`.
- Approval flow currently covers one action type only: `USER_STATUS_DISABLE`.
- Audit reads are still minimal and entity-scoped by `entityType + entityId`.
- `UserCommandService#updatePassword` remains a placeholder business error, not a completed write flow.
- There is no refresh-token flow, logout flow, or token revocation flow yet.
- Public AI now includes ticket interaction history, ticket summary/triage/internal reply draft, and import AI interaction history plus error summary plus mapping suggestion plus fix recommendation; agentic write paths are still pending.
- The interaction-history endpoint exposes ticket-scoped runtime usage/cost metadata only; it is not a tenant billing, ledger, or invoice surface.
- AI runtime still uses instance-level provider configuration only; there is no tenant BYOK, streaming, tool calling, model routing, or automatic write-back loop.
- Ticket enrichments such as priority, SLA, attachments, and notifications remain post-Week-3 follow-up work.
- Deployment-ready manifests, production secret-management guidance, and performance artifacts are still pending.
- There is no tenant admin UI or frontend yet.

## Known Gaps And Verification Notes

- `user_role` tenant consistency is not yet enforced at the database layer.
- Ticket assignee / creator / operator tenant consistency is enforced in service logic today, not yet at the database-constraint level.
- RBAC endpoints under `/api/v1/rbac/**` are still demo-oriented rather than production-oriented business APIs.
- Focused automated coverage now includes the public AI interaction-history, AI summary, AI triage, AI reply-draft, and import AI interaction-history plus error-summary plus mapping-suggestion plus fix-recommendation slices: happy path, permission failure, cross-tenant not-found behavior, history `interactionType` and `status` filters, stable `createdAt DESC, id DESC` ordering, history non-leakage assertions, import read-after-write visibility after real generation calls, symmetrical degraded-mode coverage for feature-disabled, provider-not-configured, provider-unavailable, timeout, and invalid-response paths, provider request-contract assertions, full `output[].content[]` scanning with multi-part `output_text` parsing, endpoint-specific required-field validation including import-array blank-item rejection and grounded `errorCode` checks, real-provider-path golden-sample regression checks, AI-context-window guards, import prompt sanitization checks against raw `USER_CSV` values, import `400` eligibility checks for no-failure, no-header-signal, unsupported-import-type, and no-row-signal jobs, sensitive-output rejection for fix recommendation, and no-business-side-effect assertions across all public AI endpoints.
- Live provider verification still needs manual local smoke through `.env` plus [runbooks/ai-live-smoke-test.md](runbooks/ai-live-smoke-test.md); Swagger rendering, real infra health, and modules outside the current focused path still need manual verification.
- Use [runbooks/automated-tests.md](runbooks/automated-tests.md), [runbooks/regression-checklist.md](runbooks/regression-checklist.md), and [runbooks/ai-regression-checklist.md](runbooks/ai-regression-checklist.md) for the current verification baseline.
- Use [architecture/non-blocking-backlog.md](architecture/non-blocking-backlog.md) for tracked non-blocking follow-up items that should not be lost between phases.
