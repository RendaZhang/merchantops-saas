# Testing Agent Guidance

Last updated: 2026-03-10

## Purpose

This page records the current verification and regression guidance for testing-focused agents and contributors.

Use it when changing tests, validating API behavior, or updating docs that describe current verification coverage.

## Default Handoff Sequence

When you take over an in-flight change set, use this order:

1. Inspect the staged scope first with `git diff --cached --name-only` and `git diff --cached --stat`.
2. Map the staged files to the affected test layers, choose the smallest sufficient verification set, and execute enough of it to support a staged testing report.
3. If the staged change touches controllers, security, repositories, entities, or a bug that only appears in real request flow, run `.\mvnw.cmd -pl merchantops-api -am install -DskipTests`, start the API from `merchantops-api` with `..\mvnw.cmd spring-boot:run`, and follow [../runbooks/local-smoke-test.md](../runbooks/local-smoke-test.md).
4. Use unique generated usernames for write-path smoke tests and clean `user_role` rows before deleting the corresponding `users` rows.
5. Report concrete findings first. Keep summaries and changelog-style recap secondary.

## `TT staged` Expectations

Use `TT staged` as a testing-focused staged review, not as a lightweight routing shortcut.

When handling `TT staged`:

- review the staged diff only unless the requester explicitly asks for full-worktree testing review
- inspect implementation, affected tests, and testing docs together before trusting existing coverage
- run or recommend the smallest sufficient automated and manual verification needed to support the report
- report concrete findings ordered by urgency, using `P1`, `P2`, and `P3` labels when issues are found
- if no findings remain, say that explicitly and still mention residual coverage gaps, manual-only checks, or verification that was not run
- call out when the current automated suite passed but failed to cover the bug or risk you found

## Current Coverage Baseline

The current automated baseline is centered on the completed Week 2 tenant user-management loop plus the first public Week 3 ticket-workflow slice.

Today it covers:

- login success and wrong-password failure
- real JWT parsing and permission claims
- `GET /api/v1/users`, `GET /api/v1/users/{id}`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, `GET /api/v1/roles`, and `PUT /api/v1/users/{id}/roles` authentication and permission behavior
- `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}`, `POST /api/v1/tickets`, `PATCH /api/v1/tickets/{id}/assignee`, `PATCH /api/v1/tickets/{id}/status`, and `POST /api/v1/tickets/{id}/comments` authentication and permission behavior
- controller request binding and tenant-context forwarding
- user-management query and command service behavior
- ticket query and command service behavior
- repository-backed tenant-scoped page query behavior
- operator attribution persistence on `users.created_by` and `users.updated_by`
- workflow-log persistence on `ticket_operation_log`
- stale-token rejection after user status, role, or permission changes

It does not replace manual checks for:

- Swagger/OpenAPI rendering
- real infra health such as `MySQL`, `Redis`, and `RabbitMQ`
- authenticated endpoints outside the covered login + `/api/v1/users` + `/api/v1/roles` + `/api/v1/tickets` path

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

## Testing Role Rules

- Read [../runbooks/automated-tests.md](../runbooks/automated-tests.md) before suggesting or running the default regression command.
- Use [../runbooks/local-smoke-test.md](../runbooks/local-smoke-test.md) after the automated suite passes when manual validation is still required.
- Use [../runbooks/regression-checklist.md](../runbooks/regression-checklist.md) for broader validation after security, SQL, environment, or public API changes.
- For `/api/v1/users` behavior, compare runtime and test expectations against [../reference/user-management.md](../reference/user-management.md) and [../reference/authentication-and-rbac.md](../reference/authentication-and-rbac.md).
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
