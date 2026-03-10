# Project Guidance

This repository currently organizes handoff rules around four roles:

1. Documentation
2. Testing
3. Development
4. Review and Release

## Documentation Role

- Read [docs/reference/documentation-maintenance.md](docs/reference/documentation-maintenance.md) before making documentation changes.
- Read [docs/reference/development-agent-guidance.md](docs/reference/development-agent-guidance.md) before making development-facing documentation changes.
- Keep root `README.md` high-level and move development detail into `docs/`.
- Do not document an endpoint as public unless it is visible in Swagger.
- Public API change: update reference docs, `api-demo.http`, runbooks, and `docs/project-status.md`.
- Internal groundwork only: update `docs/project-status.md` and `docs/roadmap.md`, not public API reference.
- If a new development guidance page is added or renamed, update `docs/README.md` and the relevant docs index page.
- New architecture decision: add or update an ADR and `docs/architecture/README.md`.
- New tag or release milestone: update `CHANGELOG.md` and `docs/reference/release-versioning.md`.

## Testing Role

- Start with [docs/runbooks/automated-tests.md](docs/runbooks/automated-tests.md) for the current automated regression entry point and coverage boundary.
- Preferred command for current API and user-management changes: `.\mvnw.cmd -pl merchantops-api -am test`
- Do not default to `.\mvnw.cmd -pl merchantops-api test` when in-repo dependency signatures may have changed; `-am` avoids stale local Maven artifact mismatches.
- After automated tests pass, use [docs/runbooks/local-smoke-test.md](docs/runbooks/local-smoke-test.md) for short manual validation and [docs/runbooks/regression-checklist.md](docs/runbooks/regression-checklist.md) for broader coverage.
- For `/api/v1/users` changes, compare behavior against [docs/reference/user-management.md](docs/reference/user-management.md) and [docs/reference/authentication-and-rbac.md](docs/reference/authentication-and-rbac.md).
- Current automated tests now cover login, real JWT parsing, `/api/v1/users` permission enforcement, controller/service behavior, and repository-level user-page query behavior.
- Manual verification is still required for Swagger rendering, live infra health, and authenticated endpoints outside the covered login + `/api/v1/users` path.
- If a public API contract changes, update the relevant automated tests and the related docs/runbooks in the same change.

## Development Role

- Read [docs/reference/development-agent-guidance.md](docs/reference/development-agent-guidance.md) before changing tenant-scoped repositories, services, DTOs, or user-management internals.
- For tenant-owned business data, repository methods must include `tenantId`.
- For tenant-owned business data, service public methods must accept `tenantId` explicitly.
- Controllers may read `tenantId` from request context; lower layers must not depend on implicit thread-local access for business scoping.
- Do not add repository methods such as `findByUsername(...)` or `findById(...)` for tenant business access without tenant constraints when a tenant-aware variant is required.
- Query DTOs and command DTOs must remain separated. Do not reuse write DTOs as read responses.
- For user management, keep query DTOs under `dto/user/query` and write DTOs under `dto/user/command`.
- For `users`, treat `id`, `tenantId`, `username`, and `createdAt` as immutable after creation unless the schema and business rules are intentionally redesigned.
- Unimplemented business flows must throw unified `BizException` responses, not raw `UnsupportedOperationException` or other framework/JDK exceptions.
- Treat `GET /api/v1/users` as the baseline example for tenant-aware paging and filtering.
- Current public filters for `/api/v1/users` are `username`, `status`, and `roleCode`; keep docs and examples aligned if that contract changes.
- Keep `USER_READ` as the read permission boundary unless the user explicitly asks for a permission-policy change.
- If a user-management change is internal only, update status/roadmap docs and conventions, but do not present it as public API.
- If a user-management change becomes public, update Swagger contract, [docs/reference/user-management.md](docs/reference/user-management.md), [docs/reference/authentication-and-rbac.md](docs/reference/authentication-and-rbac.md), `api-demo.http`, and runbooks in the same change.

## Review and Release Role

- Default code review scope is the staged diff only (`git diff --cached`).
- Do not mix unstaged or untracked files into review findings unless the requester explicitly asks for full-worktree review.
- If review findings exist, report the required fixes first; do not jump straight to commit, tag, or release suggestions.
- If no findings are discovered, say so explicitly and mention any remaining testing or verification gaps.
- Generate commit messages from the staged diff only, not from the whole working tree.
- Prefer Conventional Commit style such as `feat(...)`, `fix(...)`, `docs(...)`, `test(...)`, `refactor(...)`, or `chore(...)`.
- If the staged diff mixes unrelated scopes, call that out before suggesting a single commit message.
- Create or suggest release tags only from `main`.
- Create or suggest release tags only from a clean working tree.
- Before creating or suggesting a new tag, verify that `CHANGELOG.md` and `docs/reference/release-versioning.md` are consistent with that milestone.
- Prefer annotated tags (`git tag -a`) over lightweight tags.
- Do not tag a feature or topic branch unless the requester explicitly asks for it.
- For non-urgent work that is not intended to land directly on `main`, prefer a short-lived descriptive branch such as `feature/...`, `fix/...`, `docs/...`, or `test/...`.
