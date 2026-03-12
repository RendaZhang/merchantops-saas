# Project Status

Last updated: 2026-03-12

> Maintenance note: keep this page as the source of truth for current implementation reality, current public baseline, and known gaps. If the active phase, public endpoints, automated coverage, or current limitations change, update this page before mirroring the change into [roadmap.md](roadmap.md) or [project-plan.md](project-plan.md).

## Overview

MerchantOps SaaS currently sits on a completed Week 1-4 baseline and an active Week 5 import-delivery stream. The public surface now covers tenant-scoped user management, ticket workflow, audit/approval, and import jobs including failed-row replay, while AI-specific workflow features remain planned rather than public.

## Current Phase Summary

- Current phase: Week 5 Async Import And Data Operations.
- Stable completed baselines: Week 2 tenant user management, Week 3 ticket workflow, and Week 4 audit/approval.
- Week 5 Slice A is complete with import submission/list/detail plus queue backbone.
- Week 5 Slice B is complete with narrow `USER_CSV` business-row execution, row-level failure isolation, and filtered import queue reads.
- Week 5 Slice C is complete with paged import error reporting and detail-level `errorCodeCounts`.
- Week 5 Slice D is now complete with failed-row replay as a new derived import job plus `sourceJobId` lineage.
- Exact endpoint contracts live in [reference/README.md](reference/README.md); this page keeps the phase-level truth and current limits.

## Release Baseline

- Current tagged milestone: `v0.1.3` on 2026-03-12, recorded as `Week 4 complete: audit and approval baseline`.
- Previous tagged milestone: `v0.1.2` on 2026-03-11, recorded as `Week 3 complete: ticket workflow baseline`.
- Earlier milestones: `v0.1.1` on 2026-03-11 (`Week 2 complete: tenant user management loop`) and `v0.1.0` on 2026-03-09 (`Week 1 complete: foundation phase`).
- Open-source timing expectation: an early preview becomes more realistic after Week 5, while a stronger public release should wait until at least the first AI Copilot milestone in Week 6 or Week 7.

## Current Repository Baseline

### Public workflow surface

- Auth and context: `POST /api/v1/auth/login`, `GET /api/v1/user/me`, and `GET /api/v1/context`.
- User management: tenant-scoped list/detail/create/update/status/role-assignment plus role lookup and disable-request initiation.
- Ticket workflow: tenant-scoped list/detail/create/assignee/status/comment flow with queue filters and reopen support.
- Governance: entity-scoped `GET /api/v1/audit-events` plus minimal approval request queue/detail/approve/reject flow for `USER_STATUS_DISABLE`.
- Import jobs: tenant-scoped create/list/detail/replay/error-page flow with `USER_CSV` processing, filtered queue reads, quoted CSV record parsing, `errorCodeCounts`, row-level item errors, and replay-derived job lineage.

### Shared runtime and internal baseline

- Multi-module Spring Boot backend with MySQL, Redis, RabbitMQ, Flyway, request tracing, and unified API response / exception handling.
- Tenant-aware query and command services for users, tickets, approvals, and import jobs.
- JWT claim revalidation against current user status, roles, and permissions on protected requests.
- Local import file storage abstraction with after-commit queue publish, worker consumption, and system-generated replay-file writes.
- Tenant-scoped import queue reporting with stable ordering, paged failure-item reads, `requestedBy` / `hasFailures` / `errorCodeCounts` hints, and `sourceJobId` lineage on replay-derived jobs.
- Focused automated coverage for auth, user management, ticket workflow, import jobs, audit, and approval behavior.

## Active Week 5 Work

- Keep the landed `USER_CSV` schema, replay-derived job semantics, and examples aligned across Swagger, `api-demo.http`, reference docs, and runbooks.
- Use `/api/v1/import-jobs/{id}/replay-failures`, `/errors`, and detail `sourceJobId` / `errorCodeCounts` as the current Week 5 operational baseline.
- Continue Week 5 with the next narrow import follow-up such as chunk / throughput controls without pulling Week 6 AI scope forward too early.

## Current Limitations

- Import jobs currently support one business import type only: `USER_CSV`.
- The `USER_CSV` schema is fixed to `username,displayName,email,password,roleCodes`.
- Import job list now supports `status`, `importType`, `requestedBy`, and `hasFailuresOnly`; detail now exposes `sourceJobId` and `errorCodeCounts`; `/errors` supports `page`, `size`, and `errorCode`.
- Replay currently supports one path only: failed rows from a terminal `USER_CSV` source job. Whole-file replay, selective error-code replay, edited-row replay, and chunk / throughput controls are still pending.
- Import job detail still returns full `itemErrors`; a leaner large-job detail payload and broader import reporting/export flows are still deferred.
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
- Focused automated coverage exists for the current auth + user-management + ticket + import + audit + approval path, including paged import error reporting and failed-row replay, but Swagger rendering, real infra health, and modules outside that path still need manual verification.
- Use [runbooks/automated-tests.md](runbooks/automated-tests.md) and [runbooks/regression-checklist.md](runbooks/regression-checklist.md) for the current verification baseline.
- Use [architecture/non-blocking-backlog.md](architecture/non-blocking-backlog.md) for tracked non-blocking follow-up items that should not be lost between phases.
