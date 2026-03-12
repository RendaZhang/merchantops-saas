# Testing Agent Guidance

Last updated: 2026-03-11

## Purpose

This page records the current verification and regression guidance for testing-focused agents and contributors.

Use it when changing tests, validating API behavior, or updating docs that describe current verification coverage.

## Default Handoff Sequence

When you take over an in-flight change set, use this order:

1. Inspect the staged scope first with `git diff --cached --name-only` and `git diff --cached --stat`.
2. Map the staged files to the affected test layers, choose the smallest sufficient verification set, and execute enough of it to support a staged testing report.
3. If the staged change touches controllers, security, repositories, entities, or a bug that only appears in real request flow, run `.\mvnw.cmd -pl merchantops-api -am install -DskipTests`, start the API from `merchantops-api` with `..\mvnw.cmd spring-boot:run`, and follow [../runbooks/local-smoke-test.md](../runbooks/local-smoke-test.md).
4. If the staged change adds or edits a Flyway migration, verify the intended schema/data effect against the real local MySQL path rather than trusting only H2 or manually-created test schemas.
5. Use unique generated usernames for write-path smoke tests and clean `user_role` rows before deleting the corresponding `users` rows.
6. If the staged change affects status, roles, permissions, or JWT-claim propagation, verify both the stale-token rejection path and the refreshed-login success path before signing off.
7. Report concrete findings first. Keep summaries and changelog-style recap secondary.

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

The current automated baseline is centered on the completed Week 2-4 public workflow surface plus the active Week 5 import path.

Today it covers:

- login success and wrong-password failure
- real JWT parsing and permission claims
- current public authz behavior for user management, ticket workflow, audit query, approval flow, import jobs, and `GET /api/v1/roles`
- controller request binding and tenant-context forwarding for the current public workflow surface
- user, ticket, approval, and import query/command service behavior
- repository-backed tenant-scoped user page query behavior
- operator attribution persistence on user writes
- workflow-log persistence on `ticket_operation_log` and generic `audit_event` emission on covered write flows
- import queue publication, worker execution, row-level failure isolation, and import migration protection
- stale-token rejection after user status, role, or permission changes

It does not replace manual checks for:

- Swagger/OpenAPI rendering
- real infra health such as `MySQL`, `Redis`, and `RabbitMQ`
- authenticated endpoints outside the covered login + user-management + ticket + audit + approval + import path

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
- Do not default to `java -jar .\merchantops-api\target\merchantops-api-0.0.1-SNAPSHOT.jar` for local smoke. The current package is not the default runnable entry point.
- For H2 tests that depend on MySQL compatibility, keep `@AutoConfigureTestDatabase(replace = NONE)` and verify `MODE` through `INFORMATION_SCHEMA.SETTINGS` rather than through `DatabaseMetaData#getURL()`.
- Treat password formatting as a cross-flow regression point. Whenever create-user or login password handling changes, verify that both flows enforce the same rule.
- Do not let smoke docs over-claim coverage. If a runbook executes only the happy path, keep negative-path expectations in automated-test notes or the regression checklist and label them that way.
- When smoke docs use seeded IDs such as a demo assignee, say whether the value assumes a fresh local database or whether the tester should paste an ID from an earlier response.
- Do not treat successful re-login as enough evidence for authz changes. If status, role, or permission data changed, also prove that a token issued before the change is rejected on the next protected request when the current design says claims should go stale immediately.

## Testing Role Rules

- Read [../runbooks/automated-tests.md](../runbooks/automated-tests.md) before suggesting or running the default regression command.
- Use [../runbooks/local-smoke-test.md](../runbooks/local-smoke-test.md) after the automated suite passes when manual validation is still required.
- Use [../runbooks/regression-checklist.md](../runbooks/regression-checklist.md) for broader validation after security, SQL, environment, or public API changes.
- For `/api/v1/users` behavior, compare runtime and test expectations against [../reference/user-management.md](../reference/user-management.md) and [../reference/authentication-and-rbac.md](../reference/authentication-and-rbac.md).
- For access-change work such as status updates, role reassignment, permission-seed edits, or ticket-write promotion, compare runtime and test expectations against [../reference/authentication-and-rbac.md](../reference/authentication-and-rbac.md) and the affected business reference page so stale-token versus re-login behavior stays documented and verified together.
- If a public API contract changes, update the related automated tests, runbooks, and public reference docs in the same change.
- If automated coverage meaningfully expands or narrows, update [../project-status.md](../project-status.md) so the current testing reality stays accurate.

## Documentation Touchpoints

Testing-focused work commonly requires doc updates in these places:

- [../runbooks/automated-tests.md](../runbooks/automated-tests.md): test command and coverage boundary
- [../runbooks/regression-checklist.md](../runbooks/regression-checklist.md): manual validation expectations
- [../project-status.md](../project-status.md): current automation status and remaining gaps
- [documentation-maintenance.md](documentation-maintenance.md): if the routing rules themselves change

## Related Documents

- [../runbooks/automated-tests.md](../runbooks/automated-tests.md): current automated regression entry
- [../runbooks/local-smoke-test.md](../runbooks/local-smoke-test.md): short manual verification flow
- [../runbooks/regression-checklist.md](../runbooks/regression-checklist.md): broader validation checklist
- [../reference/user-management.md](../reference/user-management.md): current `/api/v1/users` contract
- [../reference/authentication-and-rbac.md](../reference/authentication-and-rbac.md): current auth and permission behavior
