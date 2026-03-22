# Project Status

Last updated: 2026-03-22

> Maintenance note: keep this page as the source of truth for current implementation reality, current public baseline, and known gaps. If the active phase, public endpoints, automated coverage, or current limitations change, update this page before mirroring the change into [roadmap.md](roadmap.md) or [project-plan.md](project-plan.md).

## Overview

MerchantOps SaaS now sits on a completed Week 1-5 baseline plus a completion-ready Week 6 AI Copilot baseline. The public surface covers tenant-scoped user management, ticket workflow, audit/approval, import jobs, and read-only ticket AI summary, triage, internal reply-draft, and interaction-history endpoints that prove the project has moved from async workflow infrastructure into a credible AI-assisted business path.

## Current Phase Summary

- Current phase: Week 6 AI Copilot For Ticket Operations, now in a completion-ready release-cut state.
- Stable completed tagged baselines: Week 2 tenant user management, Week 3 ticket workflow, Week 4 audit/approval, and Week 5 async import and data operations.
- Week 5 remains complete with import submission/list/detail/error reporting, narrowed `USER_CSV` business-row execution, filtered queue reads, failed-row replay variants, whole-file replay for full-failure sources, and derived-job lineage.
- The current worktree has brought Week 6 to four public suggestion-only or read-only ticket AI slices exposed as `GET /api/v1/tickets/{id}/ai-interactions`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, and `POST /api/v1/tickets/{id}/ai-reply-draft` under tenant scope, `TICKET_READ`, explicit prompt versioning, controlled provider degradation, dedicated AI interaction persistence, and operator-visible runtime usage/cost metadata on interaction history.
- Next active phase after the Week 6 tag cut: Week 7 AI Copilot For Import And Data Quality.
- Exact endpoint contracts live in [reference/README.md](reference/README.md); this page keeps the phase-level truth and current limits.

## Release Baseline

- Current tagged milestone: `v0.2.0-alpha` on 2026-03-19, recorded as `Week 5 complete: async import and data operations preview`.
- Prepared next tag: `v0.3.0-beta`, intended for the Week 6 AI Copilot for Ticket Operations release cut. This tag is not created yet and should not be treated as current Git reality until the tag exists.
- Previous tagged milestone: `v0.1.3` on 2026-03-12, recorded as `Week 4 complete: audit and approval baseline`.
- Earlier milestones: `v0.1.2` on 2026-03-11 (`Week 3 complete: ticket workflow baseline`), `v0.1.1` on 2026-03-11 (`Week 2 complete: tenant user management loop`), and `v0.1.0` on 2026-03-09 (`Week 1 complete: foundation phase`).
- The current worktree baseline has reached a Week 6 completion-ready state, but no Week 6 tag has been cut yet.

## Current Repository Baseline

### Public workflow surface

- Auth and context: `POST /api/v1/auth/login`, `GET /api/v1/user/me`, and `GET /api/v1/context`.
- User management: tenant-scoped list/detail/create/update/status/role-assignment plus role lookup and disable-request initiation.
- Ticket workflow: tenant-scoped list/detail/create/assignee/status/comment flow with queue filters and reopen support.
- AI-assisted ticket read path: `GET /api/v1/tickets/{id}/ai-interactions` for narrowed interaction-history visibility plus operator-visible runtime usage/cost metadata, `POST /api/v1/tickets/{id}/ai-summary` for suggestion-only summaries, `POST /api/v1/tickets/{id}/ai-triage` for suggestion-only classification and priority guidance, and `POST /api/v1/tickets/{id}/ai-reply-draft` for internal comment-style reply drafts from ticket detail context.
- Governance: entity-scoped `GET /api/v1/audit-events` plus minimal approval request queue/detail/approve/reject flow for `USER_STATUS_DISABLE`.
- Import jobs: tenant-scoped create/list/detail/replay/replay-file/selective-replay/edited-replay/error-page flow with `USER_CSV` processing, filtered queue reads, quoted CSV record parsing, `errorCodeCounts`, row-level item errors, replay-derived job lineage, scope-only replay audit metadata, and the current Week 5 reporting/replay surface.

### Shared runtime and internal baseline

- Multi-module Spring Boot backend with MySQL, Redis, RabbitMQ, Flyway, request tracing, and unified API response / exception handling.
- Tenant-aware query and command services for users, tickets, approvals, import jobs, and the current ticket interaction-history, ticket-summary, ticket-triage, and ticket reply-draft AI read paths.
- JWT claim revalidation against current user status, roles, and permissions on protected requests.
- Instance-level AI provider configuration under `merchantops.ai.*`, explicit summary, triage, and reply-draft prompt versions, timeout-based degradation, and OpenAI-compatible provider adapters for the current ticket AI slices.
- Dedicated `ai_interaction_record` persistence for AI runtime metadata, separate from `ticket_operation_log` and generic `audit_event`.
- Local import file storage abstraction with after-commit queue publish, scheduled queued-job recovery, worker consumption, stale-processing restart/fail handling, system-generated replay-file writes, and configurable chunk / recovery / row-limit controls.
- Focused automated coverage for auth, user management, ticket workflow, AI interaction history, AI summary, AI triage, AI reply draft, import jobs, audit, and approval behavior.

## Current Week 6 Public AI Baseline

- The current public Week 6 AI slices are `GET /api/v1/tickets/{id}/ai-interactions`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, and `POST /api/v1/tickets/{id}/ai-reply-draft`.
- All four slices are read-only. The three `POST` endpoints remain suggestion-only: they do not change ticket status, write comments, or trigger approvals, and the `GET` history endpoint only exposes narrowed stored metadata.
- Reply-draft semantics are explicitly internal and comment-style: the endpoint returns a structured draft plus `draftText`, but still does not create a comment or send an external message.
- The current AI context is limited to the target current-tenant ticket's core fields plus the most recent comments and workflow logs, with prompt-time truncation and explicit `earlier ... omitted` markers when older history exists.
- The interaction-history surface returns both successful and controlled-failure records with exact-match `interactionType` and `status` filters plus stable `createdAt DESC, id DESC` ordering.
- The interaction-history surface now exposes ticket-scoped runtime usage/cost metadata when present, returns those fields as `null` when unavailable, and still does not expose raw prompt text or raw provider payload.
- The current provider ownership model is instance-level configuration rather than tenant BYOK.
- The Week 6 public ticket AI baseline should now stay stable while Week 7 import and data-quality AI becomes the next active implementation phase after the Week 6 tag cut; automatic write-back remains out of scope.

## Current Limitations

- Import jobs currently support one business import type only: `USER_CSV`.
- The `USER_CSV` schema is fixed to `username,displayName,email,password,roleCodes`.
- Approval flow currently covers one action type only: `USER_STATUS_DISABLE`.
- Audit reads are still minimal and entity-scoped by `entityType + entityId`.
- `UserCommandService#updatePassword` remains a placeholder business error, not a completed write flow.
- There is no refresh-token flow, logout flow, or token revocation flow yet.
- Public AI is still limited to interaction history, summary, triage, and internal reply draft; import AI and agentic write paths are still pending.
- The interaction-history endpoint exposes ticket-scoped runtime usage/cost metadata only; it is not a tenant billing, ledger, or invoice surface.
- AI runtime still uses instance-level provider configuration only; there is no tenant BYOK, streaming, tool calling, model routing, or automatic write-back loop.
- Ticket enrichments such as priority, SLA, attachments, and notifications remain post-Week-3 follow-up work.
- Deployment-ready manifests, production secret-management guidance, and performance artifacts are still pending.
- There is no tenant admin UI or frontend yet.

## Known Gaps And Verification Notes

- `user_role` tenant consistency is not yet enforced at the database layer.
- Ticket assignee / creator / operator tenant consistency is enforced in service logic today, not yet at the database-constraint level.
- RBAC endpoints under `/api/v1/rbac/**` are still demo-oriented rather than production-oriented business APIs.
- Focused automated coverage now includes the public AI interaction-history, AI summary, AI triage, and AI reply-draft slices: happy path, permission failure, cross-tenant not-found behavior, history `interactionType` and `status` filters, stable `createdAt DESC, id DESC` ordering, history non-leakage assertions, symmetrical degraded-mode coverage for feature-disabled, provider-not-configured, provider-unavailable, timeout, and invalid-response paths, provider request-contract assertions, full `output[].content[]` scanning with multi-part `output_text` parsing, `408` or `504` timeout classification, endpoint-specific required-field validation, real-provider-path golden-sample regression checks, AI-context-window guards, and no-business-side-effect assertions across all public AI endpoints.
- Live provider verification, Swagger rendering, real infra health, and modules outside the current focused path still need manual verification.
- Use [runbooks/automated-tests.md](runbooks/automated-tests.md), [runbooks/regression-checklist.md](runbooks/regression-checklist.md), and [runbooks/ai-regression-checklist.md](runbooks/ai-regression-checklist.md) for the current verification baseline.
- Use [architecture/non-blocking-backlog.md](architecture/non-blocking-backlog.md) for tracked non-blocking follow-up items that should not be lost between phases.
