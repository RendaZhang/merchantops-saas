# Roadmap

Last updated: 2026-04-18

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

### Slice F: Tenant Actor Integrity Follow-Up

Goal: extend the Slice D defense-in-depth pattern to the highest-value ticket actor references without broad schema rewrite.

Expected scope:

- inventory ticket actor references that already have service-layer tenant validation
- choose the narrowest database-level same-tenant invariant that materially reduces cross-tenant risk
- add an additive migration, focused integration coverage, and hand-written test fixture updates for that invariant
- keep service-layer validation in place and avoid ticket workflow redesign, admin workflow changes, AI changes, refresh tokens, or broad actor rewrites

Stop condition:

- the selected cross-tenant actor binding is rejected at the database layer
- existing ticket, approval, auth, import, AI, and admin-console regressions remain green
- docs identify the resolved invariant and leave broader ticket actor constraints as later work

## Recently Closed

- Slice E: First Workflow Screen - the admin console now includes a protected `/tickets` route over the existing `GET /api/v1/tickets?page=0&size=10` API, with a shared authenticated layout, Zod-validated ticket page contract, loading/empty/error states, read-only table rendering, Vite dev-proxy smoke, and Nginx same-origin runtime smoke while keeping ticket detail, mutations, filters, pagination controls, AI actions, approval actions, refresh tokens, and backend API changes deferred.
- Slice D: Tenant Integrity Hardening - `V16__enforce_user_role_tenant_integrity.sql` now adds and backfills `user_role.tenant_id`, enforces same-tenant composite foreign keys to both `users` and `role`, updates role-binding writes to include tenant id, updates RBAC read joins to express the invariant, and adds focused database-level rejection coverage while leaving ticket actor, assignee, comment-author, and operation-log operator constraints as later follow-up work.
- Slice C: Same-Origin Admin + API Runtime Contract - `merchantops-admin-web` now builds into an Nginx static runtime container on `http://localhost:8081`, same-origin `/api/...` proxies to the API container, `docker-compose.runtime.yml` runs API plus admin on the existing infra network, the `runtime` Spring profile defines container defaults and runtime-injected secret requirements, docs/runbooks cover the secret contract and runtime smoke path, and CI now runs admin checks plus API/admin image builds while leaving CORS, cookies, refresh tokens, token rotation, real secret-manager integration, image publishing, K8s, Helm, TLS, and deployment automation deferred.
- Slice B: Server-Side Auth Session + Logout Foundation - login now creates an `auth_session`, JWTs carry required `sid` claims, protected requests validate active server-side session state before stale tenant/user/role checks, `POST /api/v1/auth/logout` revokes only the current session, and the admin console now signs out through the backend while keeping refresh tokens, cookies, rotation, logout-all, and cleanup scheduling deferred.
- Slice A: Minimal Admin Console Entry - introduced `merchantops-admin-web/` as a standalone Vite + React + TypeScript app, wired login plus current context through the existing backend, added token restoration, established workflow navigation placeholders, and documented the local frontend run path and architecture boundary.

## Candidate Next Slices

- Slice G: Authentication Lifecycle Widening - revisit refresh tokens, cookies, CORS, token rotation, and logout-all only after the same-origin runtime contract and first workflow screen are stable.
- Slice H: Next Admin Workflow Screen - add another narrow read-only or low-risk admin screen over an existing public API after the first ticket queue has settled.

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
