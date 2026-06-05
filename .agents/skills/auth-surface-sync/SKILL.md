---
name: auth-surface-sync
description: Align the repository docs for the current public authentication surface. Use when `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, `POST /api/v1/auth/logout-all`, JWT `sid`, `auth_session` cleanup or retention behavior, admin-console sign-out or restore wording, auth examples, or auth runbooks must move together with the current public auth implementation.
---

# Auth Surface Sync

## Overview

Use this skill when auth work spreads across backend reference docs, admin-console docs, runbooks, examples, and phase text at once. Keep it focused on the current public auth surface instead of broader future authentication ideas.

When the same auth change also moves planning or release framing, combine this skill with the narrower companion skill that owns that concern:

- pair it with [phase-status-sync](../phase-status-sync/SKILL.md) when `docs/project-status.md`, `docs/roadmap.md`, `docs/project-plan.md`, or `docs/product-strategy.md` need the same auth-boundary or active-slice update
- pair it with [release-tag-prep](../release-tag-prep/SKILL.md) when the same auth doc change is part of pre-tag, post-tag, release-cut, or baseline wording
- prefer [doc-staged-sync](../doc-staged-sync/SKILL.md) when the work is broader documentation routing rather than auth-surface alignment itself

Responsibility split:

- `auth-surface-sync` owns auth reference docs, admin-console auth wording, examples, runbooks, and current auth-boundary wording
- `phase-status-sync` owns the division of facts across status, roadmap, plan, and product strategy
- `release-tag-prep` owns tag baseline, changelog, release-versioning, and release-cut framing
- `doc-staged-sync` owns general documentation routing when the change is not clearly auth-centered

## Workflow

1. Read [AGENTS.md](../../../AGENTS.md), [docs/reference/authentication-and-rbac.md](../../../docs/reference/authentication-and-rbac.md), [docs/getting-started/admin-console.md](../../../docs/getting-started/admin-console.md), [docs/architecture/admin-console-architecture.md](../../../docs/architecture/admin-console-architecture.md), [../../../merchantops-admin-web/README.md](../../../merchantops-admin-web/README.md), [docs/runbooks/automated-tests.md](../../../docs/runbooks/automated-tests.md), [docs/runbooks/regression-checklist.md](../../../docs/runbooks/regression-checklist.md), [docs/runbooks/deployment-runtime-smoke-test.md](../../../docs/runbooks/deployment-runtime-smoke-test.md), and [../../../api-demo.http](../../../api-demo.http) before editing.
2. Add [docs/reference/configuration.md](../../../docs/reference/configuration.md) when auth-session cleanup cadence, retention, or runtime-injected auth settings changed.
3. Classify the auth change:
   - public auth API or Swagger contract change
   - session lifecycle or JWT claim behavior change
   - admin auth UX or local token-storage wording change
   - runtime or verification-path change
   - phase or release framing change
4. Route updates by change type:
   - public auth API: update [docs/reference/authentication-and-rbac.md](../../../docs/reference/authentication-and-rbac.md), [docs/reference/api-docs.md](../../../docs/reference/api-docs.md), [../../../api-demo.http](../../../api-demo.http), matching runbooks, and [docs/project-status.md](../../../docs/project-status.md)
   - session lifecycle or JWT behavior: update [docs/reference/authentication-and-rbac.md](../../../docs/reference/authentication-and-rbac.md), [docs/runbooks/automated-tests.md](../../../docs/runbooks/automated-tests.md), [docs/runbooks/regression-checklist.md](../../../docs/runbooks/regression-checklist.md), and [docs/project-status.md](../../../docs/project-status.md)
   - admin auth UX or storage wording: update [docs/getting-started/admin-console.md](../../../docs/getting-started/admin-console.md), [docs/architecture/admin-console-architecture.md](../../../docs/architecture/admin-console-architecture.md), [../../../merchantops-admin-web/README.md](../../../merchantops-admin-web/README.md), and any affected smoke runbook
   - runtime or verification path: update [docs/reference/configuration.md](../../../docs/reference/configuration.md), [docs/runbooks/deployment-runtime-smoke-test.md](../../../docs/runbooks/deployment-runtime-smoke-test.md), [docs/runbooks/automated-tests.md](../../../docs/runbooks/automated-tests.md), and [docs/project-status.md](../../../docs/project-status.md)
   - phase or release wording: update [docs/project-status.md](../../../docs/project-status.md), [docs/roadmap.md](../../../docs/roadmap.md), and [../../../CHANGELOG.md](../../../CHANGELOG.md) as needed
5. Keep auth wording precise:
   - call endpoints public only when they are controller-backed and Swagger-visible
   - distinguish current-session logout from logout-all and from auth-expired local cleanup
   - only describe revoke-style success when the backend confirmed it; if the frontend clears local session after failure, say other sessions may still be active
   - keep same-origin `/api` wording aligned with the current runtime contract
   - keep refresh token, cookie auth, rotation, session list, device metadata, and selective logout wording deferred unless code changed those boundaries
6. Keep verification docs realistic:
   - backend auth changes should point to `AuthSecurityIntegrationTest` first, then `.\mvnw.cmd -pl merchantops-api -am test`
   - frontend auth/admin changes should point to `npm run typecheck`, `npm run lint`, and `npm run build` in `merchantops-admin-web`
   - Flyway or runtime auth changes should point to the documented same-origin smoke path instead of claiming unit tests alone are enough
7. Finish with an auth audit:
   - `login`, `logout`, `logout-all`, JWT `sid`, auth-session cleanup, and stale-token wording still match the implementation
   - `api-demo.http` examples still line up with the current public auth endpoints
   - admin docs do not over-claim successful revoke behavior after frontend fallback cleanup
   - phase docs reflect the current auth baseline without implying later lifecycle work is already implemented

## Repo Anchors

- [docs/reference/authentication-and-rbac.md](../../../docs/reference/authentication-and-rbac.md)
- [docs/reference/api-docs.md](../../../docs/reference/api-docs.md)
- [docs/reference/configuration.md](../../../docs/reference/configuration.md)
- [docs/getting-started/admin-console.md](../../../docs/getting-started/admin-console.md)
- [docs/architecture/admin-console-architecture.md](../../../docs/architecture/admin-console-architecture.md)
- [../../../merchantops-admin-web/README.md](../../../merchantops-admin-web/README.md)
- [docs/runbooks/automated-tests.md](../../../docs/runbooks/automated-tests.md)
- [docs/runbooks/regression-checklist.md](../../../docs/runbooks/regression-checklist.md)
- [docs/runbooks/deployment-runtime-smoke-test.md](../../../docs/runbooks/deployment-runtime-smoke-test.md)
- [docs/project-status.md](../../../docs/project-status.md)
- [docs/roadmap.md](../../../docs/roadmap.md)
- [../../../CHANGELOG.md](../../../CHANGELOG.md)
- [../../../api-demo.http](../../../api-demo.http)

## Output Shape

- State the auth change type first.
- List the docs updated or confirmed already aligned.
- Call out remaining auth doc gaps, or explicitly say none remain.
