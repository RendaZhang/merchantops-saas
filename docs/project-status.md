# Project Status

Last updated: 2026-03-19

> Maintenance note: keep this page as the source of truth for current implementation reality, current public baseline, and known gaps. If the active phase, public endpoints, automated coverage, or current limitations change, update this page before mirroring the change into [roadmap.md](roadmap.md) or [project-plan.md](project-plan.md).

## Overview

MerchantOps SaaS currently sits on a completed Week 1-5 baseline and a newly active Week 6 AI Copilot planning phase. The public surface now covers tenant-scoped user management, ticket workflow, audit/approval, and import jobs including reporting, replay variants, queued-job recovery, stale-processing safeguards, and throughput controls, while AI-specific workflow features remain planned rather than public.

## Current Phase Summary

- Current phase: Week 6 AI Copilot For Ticket Operations.
- Stable completed baselines: Week 2 tenant user management, Week 3 ticket workflow, Week 4 audit/approval, and Week 5 async import and data operations.
- Week 5 is complete with import submission/list/detail/error reporting, narrowed `USER_CSV` business-row execution, filtered queue reads, failed-row replay variants, whole-file replay for full-failure sources, and derived-job lineage.
- Week 5 runtime hardening is also complete with sequential chunk execution, per-chunk counter flushes during `PROCESSING`, queued-job recovery after after-commit publish failure, stale-processing restart/fail handling, and configurable import guardrails.
- Exact endpoint contracts live in [reference/README.md](reference/README.md); this page keeps the phase-level truth and current limits.

## Release Baseline

- Current tagged milestone: `v0.2.0-alpha` on 2026-03-19, recorded as `Week 5 complete: async import and data operations preview`.
- Previous tagged milestone: `v0.1.3` on 2026-03-12, recorded as `Week 4 complete: audit and approval baseline`.
- Earlier milestones: `v0.1.2` on 2026-03-11 (`Week 3 complete: ticket workflow baseline`), `v0.1.1` on 2026-03-11 (`Week 2 complete: tenant user management loop`), and `v0.1.0` on 2026-03-09 (`Week 1 complete: foundation phase`).
- Open-source timing expectation: `v0.2.0-alpha` is the current Week 5 preview line, while a stronger public release should still wait until at least the first AI Copilot milestone in Week 6 or Week 7.

## Current Repository Baseline

### Public workflow surface

- Auth and context: `POST /api/v1/auth/login`, `GET /api/v1/user/me`, and `GET /api/v1/context`.
- User management: tenant-scoped list/detail/create/update/status/role-assignment plus role lookup and disable-request initiation.
- Ticket workflow: tenant-scoped list/detail/create/assignee/status/comment flow with queue filters and reopen support.
- Governance: entity-scoped `GET /api/v1/audit-events` plus minimal approval request queue/detail/approve/reject flow for `USER_STATUS_DISABLE`.
- Import jobs: tenant-scoped create/list/detail/replay/replay-file/selective-replay/edited-replay/error-page flow with `USER_CSV` processing, filtered queue reads, quoted CSV record parsing, `errorCodeCounts`, row-level item errors, replay-derived job lineage, scope-only replay audit metadata, and the current Week 5 reporting/replay surface.

### Shared runtime and internal baseline

- Multi-module Spring Boot backend with MySQL, Redis, RabbitMQ, Flyway, request tracing, and unified API response / exception handling.
- Tenant-aware query and command services for users, tickets, approvals, and import jobs.
- JWT claim revalidation against current user status, roles, and permissions on protected requests.
- Local import file storage abstraction with after-commit queue publish, scheduled queued-job recovery, worker consumption, stale-processing restart/fail handling, system-generated replay-file writes, and configurable chunk / recovery / row-limit controls.
- Tenant-scoped import queue reporting with stable ordering, paged failure-item reads, `requestedBy` / `hasFailures` / `errorCodeCounts` hints, `sourceJobId` lineage on replay-derived jobs, `replayMode=WHOLE_FILE` plus `selectedErrorCodes` / `editedErrorIds` / `editedRowCount` audit hints for replay variants, and counters that advance during `PROCESSING`.
- Focused automated coverage for auth, user management, ticket workflow, import jobs, audit, and approval behavior.

## Current Week 6 Entry Point

- Week 6 should start from the completed Week 5 import baseline rather than reopening Week 5 scope.
- The first Week 6 focus should be ticket AI Copilot slices such as summary, triage suggestions, and reply-draft generation under tenant, RBAC, audit, and human-oversight boundaries.
- There are still no public AI endpoints today; AI docs remain planning and governance guidance until the first Week 6 contract is intentionally exposed.

## Current Limitations

- Import jobs currently support one business import type only: `USER_CSV`.
- The `USER_CSV` schema is fixed to `username,displayName,email,password,roleCodes`.
- Import job list now supports `status`, `importType`, `requestedBy`, and `hasFailuresOnly`; detail now exposes `sourceJobId` and `errorCodeCounts`; `/errors` supports `page`, `size`, and `errorCode`.
- Replay currently supports four narrow paths from a `USER_CSV` source job: all replayable failed rows, whole-file replay for `FAILED` jobs with `successCount = 0`, replayable failed rows selected by exact `errorCode`, and caller-edited failed rows selected by exact `errorId`.
- Import job detail still returns full `itemErrors`; a leaner large-job detail payload and broader import reporting/export flows are still deferred.
- Broader import types and richer replay reporting remain post-Week-5 follow-up work instead of current baseline scope.
- Approval flow currently covers one action type only: `USER_STATUS_DISABLE`.
- Audit reads are still minimal and entity-scoped by `entityType + entityId`.
- `UserCommandService#updatePassword` remains a placeholder business error, not a completed write flow.
- There is no refresh-token flow, logout flow, or token revocation flow yet.
- There are no public AI-assisted workflow endpoints, runtime AI audit records, or code-backed AI eval datasets yet.
- Ticket enrichments such as priority, SLA, attachments, and notifications remain post-Week-3 follow-up work.
- Deployment-ready manifests, production secret-management guidance, and performance artifacts are still pending.
- There is no tenant admin UI or frontend yet.

## Known Gaps And Verification Notes

- `user_role` tenant consistency is not yet enforced at the database layer.
- Ticket assignee / creator / operator tenant consistency is enforced in service logic today, not yet at the database-constraint level.
- RBAC endpoints under `/api/v1/rbac/**` are still demo-oriented rather than production-oriented business APIs.
- Focused automated coverage exists for the current auth + user-management + ticket + import + audit + approval path, including import authz, sequential chunk execution, queued-job recovery, stale-processing redelivery handling, processing-progress counters, paged import error reporting, row-limit failure handling, failed-row replay, whole-file replay for full-failure jobs, exact error-code selective replay, and edited failed-row replay, but Swagger rendering, real infra health, and modules outside that path still need manual verification.
- Use [runbooks/automated-tests.md](runbooks/automated-tests.md) and [runbooks/regression-checklist.md](runbooks/regression-checklist.md) for the current verification baseline.
- Use [architecture/non-blocking-backlog.md](architecture/non-blocking-backlog.md) for tracked non-blocking follow-up items that should not be lost between phases.
