# Roadmap

Last updated: 2026-06-01

> Maintenance note: keep this page focused on the active release-line milestone, active slice, candidate next slices, and stop condition. Use [project-status.md](project-status.md) for current implementation reality, [product-strategy.md](product-strategy.md) for long-term strategy, and [reference/](reference/README.md) for exact public contracts.

## Current Baseline

- Current tagged milestone: `v0.8.0-beta`, recording the completed Productization Baseline beta release.
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

- Milestone: Post-`v0.8.0-beta` Workflow Recovery and Review.
- Strategic horizon: [Product Strategy](product-strategy.md) Horizon 1.
- Candidate release line: prepared next beta target `v0.9.0-beta`, with the exact tag still provisional until release-readiness or pre-tag work.
- Goal: continue turning read-only admin workflows into safe operator action loops by reusing current public import and approval APIs, without reopening completed auth transport, same-origin runtime, or release-cut scope by default.

## Active Slice

### Slice C: Import Recovery Follow-Through

Goal: add the smallest post-proposal import recovery visibility improvement now that operators can create selective replay proposals and find the resulting approval requests from the admin console.

Expected scope:

- keep this as a narrow admin-console-first workflow recovery slice
- reuse existing public import and approval APIs where possible
- consider the smallest useful follow-through such as linking approval outcomes back to derived import jobs or filtering failed rows by `errorCode`
- keep approval review semantics, approval action types, auth transport, same-origin runtime, release-cut scope, and broad import execution changes out of scope
- keep import upload, direct replay, whole-file replay, edited replay, import AI actions, source interaction selection, and AI autonomy deferred by default
- keep completed auth/session/runtime, tenant-integrity, admin-screen, and ADR work stable unless implementation evidence reveals a direct dependency
- keep per-session revocation, richer device/session management, refresh-token, cookie/session rotation, CSRF, and deployment automation deferred by default

Stop condition:

- one narrow import recovery follow-through improvement is visible in the admin console
- the slice does not change approval review semantics or widen the backend public contract beyond implementation evidence
- frontend workspace validation and any required focused smoke pass
- docs describe the new workflow recovery scope and keep deferred import replay variants separate

## Recently Closed

- Slice B: Approval Queue Filters - the admin console now adds status, action-type, and requester filter controls to `/approvals`, keeps draft input local until `Apply`, sends only normalized non-empty filters over the existing `GET /api/v1/approval-requests?page=0&size=10` query surface, validates positive whole-number `requestedBy` values before a request, keeps `Clear` available for empty filtered results, and preserves generic permission `403` inline while `401` and auth-ending `403` still use the shared session-ended path. Approval detail, approve/reject execution, pagination controls, URL query params, backend API changes, and new approval action types stayed out of scope.
- Slice A: Import Selective Replay Proposal UI - the admin console now adds a proposal panel to `/imports/:id`, validates selected `errorCodeCounts` plus optional `proposalReason`, calls the existing `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals` API, shows inline non-auth mutation errors, and links successful responses to `/approvals/:id` while keeping actual replay execution behind the existing approval review flow. Direct replay, whole-file replay, edited replay, upload, import AI actions, backend API changes, and `sourceInteractionId` selection stayed deferred.
- Release Readiness Slice: `v0.8.0-beta` release cut - the Productization Baseline `Unreleased` changelog notes moved into a dated tag section, release-versioning, README, status, roadmap, product-strategy, project-plan, and automated-test evidence now reflect the current Productization Baseline beta release, and local backend, frontend, authenticated session-management smoke, open-source entry, and remote main CI checks were completed before the annotated tag.
- Slice G-C3: Per-Session Session Management Decision - [ADR-0014](architecture/adr/0014-per-session-session-management-boundary.md) keeps the current G-C2 session-management boundary in place and defers stable public session handles, device metadata, IP, user-agent, last-seen fields, and per-session revoke controls until a later metadata/privacy/retention decision. This leaves `GET /api/v1/auth/sessions`, `POST /api/v1/auth/logout`, `POST /api/v1/auth/logout-all`, and `POST /api/v1/auth/logout-others` unchanged and keeps cookie/session rotation separate.
- Slice G-C2: Logout Other Sessions Contract - `POST /api/v1/auth/logout-others` now revokes other `ACTIVE` auth sessions for the authenticated current user in the current tenant while preserving the caller's current session, preserves other users and other tenants, adds a confirmed `/sessions` admin action that refreshes the auth-session query without clearing the local token, and records focused auth plus frontend workspace validation while leaving raw `sid`, per-session revoke handles, device metadata, refresh tokens, cookies, token rotation, and CSRF deferred.
- Slice G-C1A: Admin Session Inventory Screen - the admin console now adds a protected `/sessions` route over the existing `GET /api/v1/auth/sessions` API, validates the response with Zod, renders a read-only current-user session table with current-session marker, status, created, expiry, and revoked timestamps, and clears the sessions query cache on login, sign out, and sign-out-all while keeping raw `sid`, device metadata, per-session revoke controls, refresh-token, cookie/session rotation, CSRF, and backend API changes deferred for that slice. Slice G-C2 later added the narrow logout-other-sessions backend/UI contract.
- Slice G-C1: Current-User Session Inventory - `GET /api/v1/auth/sessions` now returns a read-only current-user session list over existing `auth_session` rows for the authenticated tenant/user, marks the current JWT `sid`, computes `ACTIVE` / `EXPIRED` / `REVOKED`, keeps rows sorted by `createdAt DESC, id DESC`, and avoids raw `sid`, device metadata, per-session revoke handles, refresh-token, cookie/session rotation, and CSRF. Slice G-C1A later added the read-only admin screen over this existing contract.
- Slice G-C0: Authentication Lifecycle Contract Decision - [ADR-0013](architecture/adr/0013-keep-admin-auth-on-bearer-session-before-cookie-rotation.md) keeps the current admin auth boundary on bearer access tokens plus server-side `auth_session` validation, kept `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, and `POST /api/v1/auth/logout-all` as the public auth surface before G-C1, deferred refresh tokens, HttpOnly cookies, access-token rotation, CSRF handling, device metadata, per-session device logout, and sign-out-other-sessions at that decision point, and sequenced the now-completed G-C1 current-user session inventory before any token transport or rotation change. Slice G-C2 later selected logout-other-sessions without changing the bearer-token decision.
- Slice I2: Ticket Child Table Tenant Linkage - `V20__enforce_ticket_child_table_tenant_linkage.sql` now adds `ticket(id, tenant_id)` uniqueness plus composite same-tenant foreign keys from `ticket_comment(ticket_id, tenant_id)` and `ticket_operation_log(ticket_id, tenant_id)` to `ticket(id, tenant_id)`, with focused migration plus ticket workflow rejection coverage and default backend regression while leaving public APIs, Swagger, DTOs, services, and admin-console behavior unchanged.
- Slice I1: Ticket Child Actor Tenant Integrity - `V19__enforce_ticket_child_actor_tenant_integrity.sql` now adds child indexes and composite same-tenant foreign keys from `ticket_comment(created_by, tenant_id)` and `ticket_operation_log(operator_id, tenant_id)` to `users(id, tenant_id)`, with focused migration plus ticket workflow rejection coverage and default backend regression while leaving public APIs, Swagger, DTOs, services, and admin-console behavior unchanged. Slice I2 later covered child-table `(ticket_id, tenant_id) -> ticket(id, tenant_id)` constraints.
- Slice H8: Ticket Comment Composer - the admin console now adds a plain internal comment composer to protected `/tickets/:id`, uses the existing `POST /api/v1/tickets/{id}/comments` API, validates comment create requests with Zod, clears input after successful submit, refreshes ticket detail plus the ticket list cache so the new comment and `COMMENTED` workflow log return from the server, and records frontend workspace validation plus mocked browser smoke while leaving ticket creation, assignment, status transitions, ticket AI actions, AI interaction-history drilldown, filters, pagination controls, backend API changes, refresh tokens, cookies, and token rotation deferred.
- Slice H7: Ticket Detail + Activity Timeline Screen - the admin console now links from `/tickets` to protected `/tickets/:id`, uses the existing `GET /api/v1/tickets/{id}` API, validates ticket detail plus comment and operation-log shapes with Zod, renders read-only ticket core fields plus separate comments and workflow operation-log sections, and records frontend workspace validation plus mocked browser layout verification while leaving ticket writes, AI actions, AI interaction history drilldown, filters, pagination controls, backend API changes, refresh tokens, cookies, and token rotation deferred by that slice.
- Slice H6: Approval Request Detail + Safe Review Controls - the admin console now links from `/approvals` to protected `/approvals/:id`, uses the existing `GET /api/v1/approval-requests/{id}`, `POST /api/v1/approval-requests/{id}/approve`, and `POST /api/v1/approval-requests/{id}/reject` APIs, validates detail and review response shapes with Zod, renders read-only approval metadata plus formatted `payloadJson`, and gates pending approve/reject through inline confirmation while leaving bulk review, pagination controls, payload editing, rejection reasons, proposal creation, backend API changes, refresh tokens, cookies, and token rotation deferred. Workflow Recovery Slice B later added queue filters.
- Slice H5: Import Job Detail + Errors Screen - the admin console now links from `/imports` to protected `/imports/:id`, uses the existing `GET /api/v1/import-jobs/{id}` and `GET /api/v1/import-jobs/{id}/errors?page=0&size=10` APIs, validates detail and failed-row page shapes with Zod, renders read-only overview/count/timing/error-code diagnostics plus the first failed-row page, and records frontend workspace validation plus mocked browser layout verification while leaving upload, replay, selective or edited replay, import AI actions, filters, pagination controls, backend API changes, refresh tokens, cookies, and token rotation deferred.
- Slice H4: AI Interactions Usage Summary Screen - the admin console now includes a protected `/ai-interactions` route over the existing `GET /api/v1/ai-interactions/usage-summary` API, with Zod-validated usage-summary schemas, a live navigation item, aggregate metric cards, read-only `byInteractionType` / `byStatus` / `byPromptVersion` breakdown tables, and frontend workspace validation while leaving filters, per-request detail, ticket/import entity history drilldown, raw prompt/provider payloads, billing/ledger semantics, write actions, backend API changes, refresh tokens, cookies, and token rotation deferred.
- Slice H3: Approvals Queue Screen - the admin console now includes a protected `/approvals` route over the existing `GET /api/v1/approval-requests?page=0&size=10` API, with Zod-validated approval-request list schemas, a live navigation item, loading/empty/error/table states, and frontend workspace validation while leaving pagination controls, bulk review, payload editing, rejection reasons, proposal creation, backend API changes, refresh tokens, cookies, and token rotation deferred. Workflow Recovery Slice B later added queue filters.
- Slice H2: Imports Queue Screen - the admin console now includes a protected `/imports` route over the existing `GET /api/v1/import-jobs?page=0&size=10` API, with Zod-validated import-job list schemas, a live navigation item, loading/empty/error/table states, and frontend workspace validation while leaving upload, detail, `/errors`, filters, pagination controls, replay, selective or edited replay, import AI actions, import AI interaction history, approval workflow UI, backend API changes, refresh tokens, cookies, and token rotation deferred.
- Slice H1: Feature Flags Control Screen - the admin console now includes a protected `/feature-flags` route over the existing `GET /api/v1/feature-flags` and `PUT /api/v1/feature-flags/{key}` API, with Zod-validated feature-flag schemas, a live navigation item, per-row enable/disable controls, query refresh after update, in-page `权限不足` handling for generic permission `403`, and frontend workspace validation while leaving cross-tenant admin, percentage rollout, environment policy, batch editing, audit detail, AI provider configuration, backend API changes, refresh tokens, cookies, and token rotation deferred.
- Slice G-B1: Logout-All Sessions Contract - `POST /api/v1/auth/logout-all` now revokes every `ACTIVE` auth session for the authenticated current user in the current tenant, including the caller's current session, preserves other users and other tenants, adds a minimal admin `Sign out all sessions` action, and records focused auth plus full regression plus frontend workspace validation while leaving refresh tokens, cookies, token rotation, device metadata, and selective device logout deferred. Slice G-C1 later added read-only current-user session inventory.
- Slice G-A: Auth Session Cleanup Scheduler - the backend now exposes `merchantops.auth.session.cleanup.*`, runs a bounded scheduled cleanup pass that deletes retention-aged expired `ACTIVE` sessions and retention-aged `REVOKED` sessions, keeps request-time auth behavior unchanged, and records focused auth regression plus same-origin runtime smoke evidence while leaving refresh tokens, cookies, and rotation deferred.
- Slice F: Tenant Actor Integrity Follow-Up - `V17__enforce_ticket_actor_tenant_integrity.sql` now adds child indexes and composite same-tenant foreign keys for `ticket.assignee_id` and `ticket.created_by` to `users(id, tenant_id)`, with focused database-level rejection coverage. Slice I1 later covered ticket comment/log actor constraints, and Slice I2 later covered child-table ticket tenant constraints.
- Slice E: First Workflow Screen - the admin console now includes a protected `/tickets` route over the existing `GET /api/v1/tickets?page=0&size=10` API, with a shared authenticated layout, Zod-validated ticket page contract, loading/empty/error states, read-only table rendering, Vite dev-proxy smoke, and Nginx same-origin runtime smoke while keeping mutations, filters, pagination controls, AI actions, approval actions, refresh tokens, and backend API changes deferred.
- Slice D: Tenant Integrity Hardening - `V16__enforce_user_role_tenant_integrity.sql` now adds and backfills `user_role.tenant_id`, enforces same-tenant composite foreign keys to both `users` and `role`, updates role-binding writes to include tenant id, updates RBAC read joins to express the invariant, and adds focused database-level rejection coverage while separating ticket actor constraints into follow-up slices.
- Slice C: Same-Origin Admin + API Runtime Contract - `merchantops-admin-web` now builds into an Nginx static runtime container on `http://localhost:8081`, same-origin `/api/...` proxies to the API container, `docker-compose.runtime.yml` runs API plus admin on the existing infra network, the `runtime` Spring profile defines container defaults and runtime-injected secret requirements, docs/runbooks cover the secret contract and runtime smoke path, and CI now runs admin checks plus API/admin image builds while leaving CORS, cookies, refresh tokens, token rotation, real secret-manager integration, image publishing, K8s, Helm, TLS, and deployment automation deferred.
- Slice B: Server-Side Auth Session + Logout Foundation - login now creates an `auth_session`, JWTs carry required `sid` claims, protected requests validate active server-side session state before stale tenant/user/role checks, `POST /api/v1/auth/logout` revokes only the current session, and the admin console now signs out through the backend while keeping refresh tokens, cookies, and rotation deferred.
- Slice A: Minimal Admin Console Entry - introduced `merchantops-admin-web/` as a standalone Vite + React + TypeScript app, wired login plus current context through the existing backend, added token restoration, established workflow navigation placeholders, and documented the local frontend run path and architecture boundary.

## Candidate Next Slices

- Slice G-C4: Cookie Or Refresh-Token Transport Decision - non-default auth lifecycle candidate; select only if the next slice explicitly needs an ADR on HttpOnly cookies, refresh tokens, CSRF handling, or token rotation.

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
