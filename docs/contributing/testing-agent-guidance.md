# Testing Agent Guidance

Last updated: 2026-04-22

## Purpose

This page records the current verification and regression guidance for testing-focused agents and contributors.

Use it when changing tests, validating API behavior, or updating docs that describe current verification coverage.

## Default Handoff Sequence

When you take over an in-flight change set, use this order:

1. Inspect the staged scope first with `git diff --cached --name-only` and `git diff --cached --stat`.
2. Map the staged files to the affected test layers, choose the smallest sufficient verification set, and execute enough of it to support a staged testing report.
3. If the staged change touches controllers, security, repositories, entities, or a bug that only appears in real request flow, run `.\mvnw.cmd -pl merchantops-api -am install -DskipTests`, start the API from `merchantops-api` with `..\mvnw.cmd spring-boot:run`, and follow [../runbooks/local-smoke-test.md](../runbooks/local-smoke-test.md).
4. If the staged change touches Docker delivery, container startup, env injection behavior, admin image packaging, or same-origin proxying, also run the documented runtime compose path and verify at least `/health`, admin page load, login, context, the current protected admin workflow route such as `/tickets`, sign-out, and old-token rejection.
5. If the staged change touches CI workflow files, compare the workflow commands against [../runbooks/automated-tests.md](../runbooks/automated-tests.md) and keep the documented CI boundary explicit.
6. If the staged change touches AI provider wiring, `.env` loading, provider selection, or live vendor compatibility, use [../runbooks/ai-live-smoke-test.md](../runbooks/ai-live-smoke-test.md) plus [../runbooks/ai-regression-checklist.md](../runbooks/ai-regression-checklist.md) after the normal non-AI smoke path when needed. When the same change also targets Docker delivery or explicit container env injection, prefer running the summary-first live smoke against the Dockerized API path instead of switching back to `spring-boot:run`.
7. If the staged change adds or edits a Flyway migration, verify the intended schema/data effect against the real local MySQL path rather than trusting only H2 or manually-created test schemas.
8. Use unique generated usernames for write-path smoke tests and clean `user_role` rows before deleting the corresponding `users` rows.
9. If the staged change affects status, roles, permissions, or JWT-claim propagation, verify both the stale-token rejection path and the refreshed-login success path before signing off.
10. Report concrete findings first. Keep summaries and changelog-style recap secondary.

## `TT staged` Expectations

Use `TT staged` as a testing-focused staged review, not as a lightweight routing shortcut.

When handling `TT staged`:

- review the staged diff only unless the requester explicitly asks for full-worktree testing review
- inspect implementation, affected tests, and testing docs together before trusting existing coverage
- run or recommend the smallest sufficient automated and manual verification needed to support the report
- report concrete findings ordered by urgency, using `P1`, `P2`, and `P3` labels when issues are found
- if no findings remain, say that explicitly and still mention residual coverage gaps, manual-only checks, or verification that was not run
- call out when the current automated suite passed but failed to cover the bug or risk you found

## `TT last` Expectations

Use `TT last` when the requester wants a testing-focused review of the most recent commit rather than the current staged diff.

When handling `TT last`:

- treat `HEAD^..HEAD` as the default review scope unless the requester names a different commit
- inspect implementation, affected tests, and testing docs together before trusting existing coverage
- run or recommend the smallest sufficient automated and manual verification needed to support the report
- report concrete findings ordered by urgency, using `P1`, `P2`, and `P3` labels when issues are found
- if no findings remain, say that explicitly and still mention residual coverage gaps, manual-only checks, or verification that was not run

## Current Coverage Baseline

The current automated baseline is centered on the completed Week 2-8 public workflow surface, the completed Week 9 AI governance, eval, cost, and usage baseline, the completed Week 10 Slice A persisted feature-flag hardening baseline, the Productization Baseline server-side auth-session/logout, status-aware auth-session cleanup, same-origin runtime foundation, `user_role` tenant-integrity hardening, first read-only admin Tickets screen, and root ticket actor tenant-integrity hardening, plus the no-secret CI quality gate.

Today it covers:

- login success and wrong-password failure
- server-side auth-session creation, required JWT `sid`, current-session logout revocation, sidless/revoked/expired-session `401` behavior, retention-window cleanup of old `ACTIVE` and `REVOKED` auth-session rows, post-cleanup old-token `401` behavior, and independent multi-session behavior
- database-level rejection of cross-tenant `user_role` bindings
- database-level rejection of cross-tenant root ticket assignee/creator bindings while nullable ticket assignees remain valid
- real JWT parsing and permission claims
- current public feature-flag management behavior for `GET /api/v1/feature-flags` and `PUT /api/v1/feature-flags/{key}`
- current public authz behavior for user management, ticket workflow, audit query, approval flow, import jobs, ticket AI interaction-history plus summary/triage/reply-draft, tenant AI usage-summary, import AI interaction-history plus error summary/mapping suggestion/fix recommendation, and `GET /api/v1/roles`
- controller request binding and tenant-context forwarding for the current public workflow surface
- user, ticket, approval, and import query/command service behavior
- repository-backed tenant-scoped user page query behavior, including role joins through `user_role.tenant_id`
- operator attribution persistence on user writes
- workflow-log persistence on `ticket_operation_log` and generic `audit_event` emission on covered write flows
- import queue publication, worker execution, row-level failure isolation, and import migration protection
- Week 8 approval-backed import selective replay proposals plus ticket comment proposals, including shared pending-request-key uniqueness hardening, duplicate suppression, reproposal-after-resolution, and mixed-action approval review behavior
- Week 10 persisted feature-flag hardening across the six AI generation endpoints plus both Week 8 workflow proposal bridges, including list/update API happy path, `FEATURE_FLAG_MANAGE` denial, stale-claim re-login, unknown-key `404`, idempotent no-op updates, audit snapshots, controlled `503` disable behavior, and no cross-flag leakage
- provider-adapter failure handling for the current public ticket AI summary, triage, and reply-draft paths plus the current public import AI error-summary, mapping-suggestion, and fix-recommendation paths
- symmetric degraded-mode persistence and no-side-effect assertions across the current public ticket and import AI endpoints
- symmetric AI hardening parity across the current public ticket and import AI endpoints, including provider-not-configured, provider-unavailable, timeout, invalid-response or output-policy-validation failure paths, `ai_interaction_record.status` assertions, response-shape / golden-sample expectations, import prompt sanitization, and fix-recommendation sensitive-output rejection
- Week 9 shared AI governance baseline through the executable six-workflow prompt inventory, shared comparator pass, golden plus failure plus policy datasets, and tenant-scoped AI usage-summary aggregate coverage
- stale-token rejection after user status, role, or permission changes, plus refreshed-login success when newly granted access now includes `FEATURE_FLAG_MANAGE`
- GitHub Actions CI parity for the default Maven regression command, admin frontend typecheck/lint/build, and Docker image construction for both API and admin web images
- focused admin-console smoke coverage for Vite dev-proxy and same-origin runtime paths, including login, context, `/tickets`, sign-out, and old-token rejection

It does not replace manual checks for:

- Swagger/OpenAPI rendering
- real infra health such as `MySQL`, `Redis`, and `RabbitMQ`
- Dockerized API/admin container startup, shared-network connectivity, same-origin `/api` proxying, and authenticated runtime smoke on the documented shared-network path
- authenticated endpoints outside the covered login/session/logout + user-management + ticket + audit + approval + import path

## Default Test Entry Point

Start with:

```powershell
.\mvnw.cmd -pl merchantops-api -am test
```

Why this is the default:

- `merchantops-api` currently depends on in-repo changes from `merchantops-infra`
- `-am` avoids stale local Maven artifact mismatches when signatures changed in dependent modules

Use the full reactor only when broader verification is needed:

```powershell
.\mvnw.cmd test
```

## Known Pitfalls

- `spring-boot:run` does not magically use every fresh sibling-module compile output. If `merchantops-infra` signatures changed, install the reactor modules first.
- The packaged Spring Boot jar is now valid for Docker delivery and other explicit `java -jar` entrypoints. Local smoke still defaults to `spring-boot:run`; use the Dockerized API path for API-only container proof, and use the runtime compose path when the change must prove admin image startup, explicit runtime env injection, or same-origin `/api` proxying.
- For H2 tests that depend on MySQL compatibility, keep `@AutoConfigureTestDatabase(replace = NONE)` and verify `MODE` through `INFORMATION_SCHEMA.SETTINGS` rather than through `DatabaseMetaData#getURL()`.
- Treat password formatting as a cross-flow regression point. Whenever create-user or login password handling changes, verify that both flows enforce the same rule.
- Do not let smoke docs over-claim coverage. If a runbook executes only the happy path, keep negative-path expectations in automated-test notes or the regression checklist and label them that way.
- When smoke docs use seeded IDs such as a demo assignee, say whether the value assumes a fresh local database or whether the tester should paste an ID from an earlier response.
- Do not treat successful re-login as enough evidence for authz changes. If status, role, or permission data changed, also prove that a token issued before the change is rejected on the next protected request when the current design says claims should go stale immediately.

## Testing Role Rules

- Read [../runbooks/automated-tests.md](../runbooks/automated-tests.md) before suggesting or running the default regression command.
- Use [../runbooks/local-smoke-test.md](../runbooks/local-smoke-test.md) after the automated suite passes when manual validation is still required.
- Use [../runbooks/ai-live-smoke-test.md](../runbooks/ai-live-smoke-test.md) when AI provider wiring, `.env` bootstrap, provider normalization, or real-vendor compatibility changed. Pair it with [../runbooks/ai-regression-checklist.md](../runbooks/ai-regression-checklist.md).
- Use [../runbooks/regression-checklist.md](../runbooks/regression-checklist.md) for broader validation after security, SQL, environment, or public API changes.
- For `/api/v1/users` behavior, compare runtime and test expectations against [../reference/user-management.md](../reference/user-management.md) and [../reference/authentication-and-rbac.md](../reference/authentication-and-rbac.md).
- For access-change work such as status updates, role reassignment, permission-seed edits, or ticket-write promotion, compare runtime and test expectations against [../reference/authentication-and-rbac.md](../reference/authentication-and-rbac.md) and the affected business reference page so stale-token versus re-login behavior stays documented and verified together.
- When a slice exposes multiple parallel public AI endpoints, keep the hardening symmetric across them rather than letting one endpoint become the only fully hardened reference.
- That symmetry should explicitly cover adapter failure tests, integration degraded-mode coverage, workflow-safety or no-side-effect assertions, AI runbook/checklist expectations, provider-not-configured and provider-unavailable paths, timeout behavior, invalid-response or output-policy-validation failures, `ai_interaction_record.status` assertions, and response-shape / golden-sample parity.
- When an AI response includes grounded identifiers or observed signals, output-policy coverage should prove those values belong to the local sanitized input or other known tenant-scoped facts, not just that they are present and well-typed.
- If a public API contract changes, update the related automated tests, runbooks, and public reference docs in the same change.
- If automated coverage meaningfully expands or narrows, update [../project-status.md](../project-status.md) so the current testing reality stays accurate.

## Documentation Touchpoints

Testing-focused work commonly requires doc updates in these places:

- [../runbooks/automated-tests.md](../runbooks/automated-tests.md): test command and coverage boundary
- [../runbooks/ai-live-smoke-test.md](../runbooks/ai-live-smoke-test.md): local provider live smoke path and `.env` setup sequence
- [../runbooks/regression-checklist.md](../runbooks/regression-checklist.md): manual validation expectations
- [../project-status.md](../project-status.md): current automation status and remaining gaps
- [documentation-maintenance.md](documentation-maintenance.md): if the routing rules themselves change

## Related Documents

- [../runbooks/automated-tests.md](../runbooks/automated-tests.md): current automated regression entry
- [../runbooks/local-smoke-test.md](../runbooks/local-smoke-test.md): short manual verification flow
- [../runbooks/ai-live-smoke-test.md](../runbooks/ai-live-smoke-test.md): local provider live smoke path
- [../runbooks/regression-checklist.md](../runbooks/regression-checklist.md): broader validation checklist
- [../reference/user-management.md](../reference/user-management.md): current `/api/v1/users` contract
- [../reference/authentication-and-rbac.md](../reference/authentication-and-rbac.md): current auth and permission behavior
