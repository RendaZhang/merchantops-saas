# Testing Agent Guidance

Last updated: 2026-03-10

## Purpose

This page records the current verification and regression guidance for testing-focused agents and contributors.

Use it when changing tests, validating API behavior, or updating docs that describe current verification coverage.

## Default Handoff Sequence

When you take over an in-flight change set, use this order:

1. Inspect the staged scope first with `git diff --cached --name-only` and `git diff --cached --stat`.
2. Map the staged files to the affected test layers, then run `.\mvnw.cmd -pl merchantops-api -am test`.
3. If the staged change touches controllers, security, repositories, entities, or a bug that only appears in real request flow, run `.\mvnw.cmd -pl merchantops-api -am install -DskipTests`, start the API from `merchantops-api` with `..\mvnw.cmd spring-boot:run`, and follow [../runbooks/local-smoke-test.md](../runbooks/local-smoke-test.md).
4. Use unique generated usernames for write-path smoke tests and clean `user_role` rows before deleting the corresponding `users` rows.
5. Report concrete findings first. Keep summaries and changelog-style recap secondary.

## Current Coverage Baseline

The current automated baseline is centered on the Week 2 tenant user-management loop.

Today it covers:

- login success and wrong-password failure
- real JWT parsing and permission claims
- `GET /api/v1/users` and `POST /api/v1/users` authentication and permission behavior
- controller request binding and tenant-context forwarding
- user-management query and command service behavior
- repository-backed tenant-scoped page query behavior

It does not replace manual checks for:

- Swagger/OpenAPI rendering
- real infra health such as `MySQL`, `Redis`, and `RabbitMQ`
- authenticated endpoints outside the covered login + `/api/v1/users` (`GET` and `POST`) path

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
