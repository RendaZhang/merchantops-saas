# Roadmap

Last updated: 2026-04-16

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

### Slice D: Tenant Integrity Hardening

Goal: move the highest-value tenant consistency guarantees from service-only checks toward database-backed constraints now that the admin, auth-session, and production-like runtime boundary are stable.

Expected scope:

- review the access-control and tenant-integrity gaps tracked in [architecture/access-control-evolution-plan.md](architecture/access-control-evolution-plan.md)
- add the narrowest safe database-backed constraint or migration for a high-risk tenant consistency gap, starting with `user_role` or ticket actor/assignee relationships only if the existing data and H2/MySQL test fixtures can support it cleanly
- keep service-layer tenant checks in place and treat database constraints as defense-in-depth rather than a replacement for request-time validation
- update migration docs, test fixtures, and regression guidance for any new tenant-integrity invariant

Stop condition:

- the selected tenant-integrity invariant is enforced by migration plus automated tests
- existing auth/session, stale-token, import, ticket, and approval regressions remain green
- docs clearly distinguish the new database invariant from still-service-only tenant checks
- follow-up tenant-integrity gaps remain tracked without widening into a broad schema rewrite

## Recently Closed

- Slice C: Same-Origin Admin + API Runtime Contract - `merchantops-admin-web` now builds into an Nginx static runtime container on `http://localhost:8081`, same-origin `/api/...` proxies to the API container, `docker-compose.runtime.yml` runs API plus admin on the existing infra network, the `runtime` Spring profile defines container defaults and runtime-injected secret requirements, docs/runbooks cover the secret contract and runtime smoke path, and CI now runs admin checks plus API/admin image builds while leaving CORS, cookies, refresh tokens, token rotation, real secret-manager integration, image publishing, K8s, Helm, TLS, and deployment automation deferred.
- Slice B: Server-Side Auth Session + Logout Foundation - login now creates an `auth_session`, JWTs carry required `sid` claims, protected requests validate active server-side session state before stale tenant/user/role checks, `POST /api/v1/auth/logout` revokes only the current session, and the admin console now signs out through the backend while keeping refresh tokens, cookies, rotation, logout-all, and cleanup scheduling deferred.
- Slice A: Minimal Admin Console Entry - introduced `merchantops-admin-web/` as a standalone Vite + React + TypeScript app, wired login plus current context through the existing backend, added token restoration, established workflow navigation placeholders, and documented the local frontend run path and architecture boundary.

## Candidate Next Slices

- Slice E: First Workflow Screen - add the narrowest useful admin-console data page after the session boundary is settled, starting with a read-only queue or summary view over an existing public API.
- Slice F: Authentication Lifecycle Widening - revisit refresh tokens, cookies, CORS, token rotation, and logout-all only after the deployment and secret-management contract is clear.

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
