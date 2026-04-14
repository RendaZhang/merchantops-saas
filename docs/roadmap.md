# Roadmap

Last updated: 2026-04-14

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

### Slice B: Authentication Lifecycle Completion

Goal: turn the newly introduced admin-console session boundary into a clearer authentication lifecycle plan or implementation slice.

Expected scope:

- decide the refresh-token, logout, and token-revocation boundary for the backend and admin console
- document whether Slice B is an implementation slice or an architecture/API decision slice before widening frontend workflow pages
- keep stale-claim rejection and request-time access revalidation intact
- update auth/RBAC reference docs, getting-started docs, and admin-console docs if the public auth contract changes

Stop condition:

- refresh/logout/revocation behavior is explicitly implemented or explicitly deferred with a documented contract
- the admin console session behavior matches the documented backend contract
- stale-token, refreshed-login, and local session-clearing expectations are covered by the smallest sufficient automated or smoke checks
- docs and runbooks no longer describe the Slice A local-token-only boundary as the intended final session model

## Recently Closed

- Slice A: Minimal Admin Console Entry - introduced `merchantops-admin-web/` as a standalone Vite + React + TypeScript app, wired login plus current context through the existing backend, added token restoration, established workflow navigation placeholders, and documented the local frontend run path and architecture boundary.

## Candidate Next Slices

- Slice C: Deployment And Secret Management Path - document and harden the production-like environment contract, secret handling, and smoke-test boundary beyond the current local Docker baseline.
- Slice D: Tenant Integrity Hardening - move high-value tenant consistency guarantees from service-only checks toward database-backed constraints, starting with the access-control gaps tracked in [architecture/access-control-evolution-plan.md](architecture/access-control-evolution-plan.md).
- Slice E: First Workflow Screen - add the narrowest useful admin-console data page after the session boundary is settled, starting with a read-only queue or summary view over an existing public API.

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
