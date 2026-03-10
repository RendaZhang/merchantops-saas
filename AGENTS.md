# Project Guidance

## Documentation Quick Entry

- Read [docs/reference/documentation-maintenance.md](docs/reference/documentation-maintenance.md) before making documentation changes.
- Keep `README.md` high-level and move detail into `docs/`.
- Do not document an endpoint as public unless it is visible in Swagger.
- Public API change: update reference docs, `api-demo.http`, runbooks, and `docs/project-status.md`.
- Internal groundwork only: update `docs/project-status.md` and `docs/roadmap.md`, not public API reference.
- New architecture decision: add or update an ADR and `docs/architecture/README.md`.
- New tag or release milestone: update `CHANGELOG.md` and `docs/reference/release-versioning.md`.

## Testing Guidance

- Start with [docs/runbooks/automated-tests.md](docs/runbooks/automated-tests.md) for the current automated regression entry point and coverage boundary.
- Preferred command for current API and user-management changes: `.\mvnw.cmd -pl merchantops-api -am test`
- Do not default to `.\mvnw.cmd -pl merchantops-api test` when in-repo dependency signatures may have changed; `-am` avoids stale local Maven artifact mismatches.
- After automated tests pass, use [docs/runbooks/local-smoke-test.md](docs/runbooks/local-smoke-test.md) for short manual validation and [docs/runbooks/regression-checklist.md](docs/runbooks/regression-checklist.md) for broader coverage.
- For `/api/v1/users` changes, compare behavior against [docs/reference/user-management.md](docs/reference/user-management.md) and [docs/reference/authentication-and-rbac.md](docs/reference/authentication-and-rbac.md).
- Current automated tests now cover login, real JWT parsing, `/api/v1/users` permission enforcement, controller/service behavior, and repository-level user-page query behavior.
- Manual verification is still required for Swagger rendering, live infra health, and authenticated endpoints outside the covered login + `/api/v1/users` path.
- If a public API contract changes, update the relevant automated tests and the related docs/runbooks in the same change.
