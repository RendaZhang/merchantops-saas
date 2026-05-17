# Roadmap

Last updated: 2026-05-17

> Maintenance note: keep this page focused on the active release-line milestone, active slice, candidate next slices, and stop condition. Use [project-status.md](project-status.md) for current implementation reality, [product-strategy.md](product-strategy.md) for long-term strategy, and [reference/](reference/README.md) for exact public contracts.

## Current Baseline

- Current tagged milestone: `v0.7.0-beta`, recording the completed Week 10 Delivery Hardening and Portfolio Packaging beta baseline.
- Foundation status: Week 1-10 are complete and now treated as the archived foundation build-out.
- Current public surface and limitations live in [project-status.md](project-status.md) and the matching pages under [reference/](reference/README.md).
- Long-term product direction lives in [product-strategy.md](product-strategy.md).

## Release-Line Planning Format

Future roadmap updates should use a milestone-and-slice format rather than rebuilding another fixed 10-week plan:

- Current milestone: the active release-line target or planning boundary.
- Active slice: the single narrow implementation or documentation slice currently being worked.
- Candidate next slices: the next one to three evidence-backed slices, grouped by strategic track when helpful.
- Stop condition: what must be true before the slice can close, including implementation, tests, public docs, examples, runbooks, and release evidence when applicable.

## Current Milestone

- Milestone: Productization Baseline.
- Strategic horizon: [Product Strategy](product-strategy.md) Horizon 1.
- Candidate release line: the next beta milestone after `v0.7.0-beta`; keep the exact tag unset until release-readiness or pre-tag work.
- Goal: make the project easier to run, demonstrate, and evaluate without widening AI autonomy or adding unrelated modules by default.

## Active Slice

### Next Slice Selection Pending

Goal: select the next narrow Productization Baseline implementation slice after Slice I1 delivered ticket child actor tenant-integrity hardening.

Expected scope:

- keep the existing same-origin admin/API runtime contract intact
- prefer narrow backend hardening or existing Swagger-visible admin workflows unless the selected slice directly requires broader scope
- keep child-table ticket tenant linkage separate from completed root/child actor linkage if continuing Slice I work
- avoid broader ticket creation, assignment, status-transition work, refresh-token work, cookie/session rotation, deployment automation, or AI autonomy changes unless the selected slice directly requires them

Stop condition:

- the next slice is named with scope, stop condition, validation, and documentation expectations
- the selected slice is small enough to implement without reopening completed auth/session/runtime or admin-screen work
- docs continue to distinguish implemented schema hardening and admin screens from deferred workflow depth

## Recently Closed

- Slice I1: Ticket Child Actor Tenant Integrity - `V19__enforce_ticket_child_actor_tenant_integrity.sql` now adds child indexes and composite same-tenant foreign keys from `ticket_comment(created_by, tenant_id)` and `ticket_operation_log(operator_id, tenant_id)` to `users(id, tenant_id)`, with focused migration plus ticket workflow rejection coverage and default backend regression while leaving public APIs, Swagger, DTOs, services, admin-console behavior, and child-table `(ticket_id, tenant_id) -> ticket(id, tenant_id)` constraints unchanged.
- Slice H8: Ticket Comment Composer - the admin console now adds a plain internal comment composer to protected `/tickets/:id`, uses the existing `POST /api/v1/tickets/{id}/comments` API, validates comment create requests with Zod, clears input after successful submit, refreshes ticket detail plus the ticket list cache so the new comment and `COMMENTED` workflow log return from the server, and records frontend workspace validation plus mocked browser smoke while leaving ticket creation, assignment, status transitions, ticket AI actions, AI interaction-history drilldown, filters, pagination controls, backend API changes, refresh tokens, cookies, and token rotation deferred.
- Slice H7: Ticket Detail + Activity Timeline Screen - the admin console now links from `/tickets` to protected `/tickets/:id`, uses the existing `GET /api/v1/tickets/{id}` API, validates ticket detail plus comment and operation-log shapes with Zod, renders read-only ticket core fields plus separate comments and workflow operation-log sections, and records frontend workspace validation plus mocked browser layout verification while leaving ticket writes, AI actions, AI interaction history drilldown, filters, pagination controls, backend API changes, refresh tokens, cookies, and token rotation deferred by that slice.
- Slice H6: Approval Request Detail + Safe Review Controls - the admin console now links from `/approvals` to protected `/approvals/:id`, uses the existing `GET /api/v1/approval-requests/{id}`, `POST /api/v1/approval-requests/{id}/approve`, and `POST /api/v1/approval-requests/{id}/reject` APIs, validates detail and review response shapes with Zod, renders read-only approval metadata plus formatted `payloadJson`, and gates pending approve/reject through inline confirmation while leaving bulk review, filters, pagination controls, payload editing, rejection reasons, proposal creation, backend API changes, refresh tokens, cookies, and token rotation deferred.
- Slice H5: Import Job Detail + Errors Screen - the admin console now links from `/imports` to protected `/imports/:id`, uses the existing `GET /api/v1/import-jobs/{id}` and `GET /api/v1/import-jobs/{id}/errors?page=0&size=10` APIs, validates detail and failed-row page shapes with Zod, renders read-only overview/count/timing/error-code diagnostics plus the first failed-row page, and records frontend workspace validation plus mocked browser layout verification while leaving upload, replay, selective or edited replay, import AI actions, filters, pagination controls, backend API changes, refresh tokens, cookies, and token rotation deferred.
- Slice H4: AI Interactions Usage Summary Screen - the admin console now includes a protected `/ai-interactions` route over the existing `GET /api/v1/ai-interactions/usage-summary` API, with Zod-validated usage-summary schemas, a live navigation item, aggregate metric cards, read-only `byInteractionType` / `byStatus` / `byPromptVersion` breakdown tables, and frontend workspace validation while leaving filters, per-request detail, ticket/import entity history drilldown, raw prompt/provider payloads, billing/ledger semantics, write actions, backend API changes, refresh tokens, cookies, and token rotation deferred.
- Slice H3: Approvals Queue Screen - the admin console now includes a protected `/approvals` route over the existing `GET /api/v1/approval-requests?page=0&size=10` API, with Zod-validated approval-request list schemas, a live navigation item, loading/empty/error/table states, and frontend workspace validation while leaving approval filters, pagination controls, bulk review, payload editing, rejection reasons, proposal creation, backend API changes, refresh tokens, cookies, and token rotation deferred.
- Slice H2: Imports Queue Screen - the admin console now includes a protected `/imports` route over the existing `GET /api/v1/import-jobs?page=0&size=10` API, with Zod-validated import-job list schemas, a live navigation item, loading/empty/error/table states, and frontend workspace validation while leaving upload, detail, `/errors`, filters, pagination controls, replay, selective or edited replay, import AI actions, import AI interaction history, approval workflow UI, backend API changes, refresh tokens, cookies, and token rotation deferred.
- Slice H1: Feature Flags Control Screen - the admin console now includes a protected `/feature-flags` route over the existing `GET /api/v1/feature-flags` and `PUT /api/v1/feature-flags/{key}` API, with Zod-validated feature-flag schemas, a live navigation item, per-row enable/disable controls, query refresh after update, in-page `权限不足` handling for generic permission `403`, and frontend workspace validation while leaving cross-tenant admin, percentage rollout, environment policy, batch editing, audit detail, AI provider configuration, backend API changes, refresh tokens, cookies, and token rotation deferred.
- Slice G-B1: Logout-All Sessions Contract - `POST /api/v1/auth/logout-all` now revokes every `ACTIVE` auth session for the authenticated current user in the current tenant, including the caller's current session, preserves other users and other tenants, adds a minimal admin `Sign out all sessions` action, and records focused auth plus full regression plus frontend workspace validation while leaving refresh tokens, cookies, token rotation, session lists, device metadata, and selective device logout deferred.
- Slice G-A: Auth Session Cleanup Scheduler - the backend now exposes `merchantops.auth.session.cleanup.*`, runs a bounded scheduled cleanup pass that deletes retention-aged expired `ACTIVE` sessions and retention-aged `REVOKED` sessions, keeps request-time auth behavior unchanged, and records focused auth regression plus same-origin runtime smoke evidence while leaving refresh tokens, cookies, and rotation deferred.
- Slice F: Tenant Actor Integrity Follow-Up - `V17__enforce_ticket_actor_tenant_integrity.sql` now adds child indexes and composite same-tenant foreign keys for `ticket.assignee_id` and `ticket.created_by` to `users(id, tenant_id)`, with focused database-level rejection coverage. Slice I1 later covered ticket comment/log actor constraints, while child-table ticket tenant constraints remain later work.
- Slice E: First Workflow Screen - the admin console now includes a protected `/tickets` route over the existing `GET /api/v1/tickets?page=0&size=10` API, with a shared authenticated layout, Zod-validated ticket page contract, loading/empty/error states, read-only table rendering, Vite dev-proxy smoke, and Nginx same-origin runtime smoke while keeping mutations, filters, pagination controls, AI actions, approval actions, refresh tokens, and backend API changes deferred.
- Slice D: Tenant Integrity Hardening - `V16__enforce_user_role_tenant_integrity.sql` now adds and backfills `user_role.tenant_id`, enforces same-tenant composite foreign keys to both `users` and `role`, updates role-binding writes to include tenant id, updates RBAC read joins to express the invariant, and adds focused database-level rejection coverage while separating ticket actor constraints into follow-up slices.
- Slice C: Same-Origin Admin + API Runtime Contract - `merchantops-admin-web` now builds into an Nginx static runtime container on `http://localhost:8081`, same-origin `/api/...` proxies to the API container, `docker-compose.runtime.yml` runs API plus admin on the existing infra network, the `runtime` Spring profile defines container defaults and runtime-injected secret requirements, docs/runbooks cover the secret contract and runtime smoke path, and CI now runs admin checks plus API/admin image builds while leaving CORS, cookies, refresh tokens, token rotation, real secret-manager integration, image publishing, K8s, Helm, TLS, and deployment automation deferred.
- Slice B: Server-Side Auth Session + Logout Foundation - login now creates an `auth_session`, JWTs carry required `sid` claims, protected requests validate active server-side session state before stale tenant/user/role checks, `POST /api/v1/auth/logout` revokes only the current session, and the admin console now signs out through the backend while keeping refresh tokens, cookies, and rotation deferred.
- Slice A: Minimal Admin Console Entry - introduced `merchantops-admin-web/` as a standalone Vite + React + TypeScript app, wired login plus current context through the existing backend, added token restoration, established workflow navigation placeholders, and documented the local frontend run path and architecture boundary.

## Candidate Next Slices

- Slice I2: Ticket Child Table Tenant Linkage - consider `(ticket_id, tenant_id) -> ticket(id, tenant_id)` constraints for `ticket_comment` and `ticket_operation_log` without reopening completed root/child actor invariants.
- Slice G-C: Authentication Lifecycle Contract Follow-Up - decide whether refresh-token or cookie/session rotation is now justified by the same-origin runtime model, keeping either as a separate auth-contract slice.

## Default Deferrals

- Do not add more AI endpoints or higher-autonomy agent behavior by default.
- Do not build full billing, ledger, invoice, K8s, Helm, image publishing, or generic feature-flag platform scope before the Productization Baseline slice sequence creates a concrete need.
- Do not expand import types, ticket SLA/priority/attachments, or additional approval action types until the productization baseline is stable enough to demo and run.

## Release Evidence Expectations

- Default backend regression remains `.\mvnw.cmd -pl merchantops-api -am test`.
- Keep Docker build and local Docker smoke evidence when delivery or deployment docs change.
- Frontend workspace verification is `npm run typecheck`, `npm run lint`, and `npm run build` from `merchantops-admin-web`.
- Update [CHANGELOG.md](../CHANGELOG.md), [docs/contributing/release-versioning.md](contributing/release-versioning.md), and current-baseline docs only when preparing a release-cut or tag.

## Notes

- This document tracks intended next work, not committed delivery dates.
- Historical implementation detail belongs in [project-status.md](project-status.md), [archive/completed-10-week-foundation-plan.md](archive/completed-10-week-foundation-plan.md), and [CHANGELOG.md](../CHANGELOG.md), not here.
