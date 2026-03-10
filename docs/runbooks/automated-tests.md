# Automated Tests

Last updated: 2026-03-10

Use this runbook when you want a fast regression signal before doing manual API verification.

## Recommended Commands

Preferred command for the current user-management work:

```powershell
.\mvnw.cmd -pl merchantops-api -am test
```

Why this is the default:

- `merchantops-api` currently depends on in-repo changes from `merchantops-infra`
- `-am` (`--also-make`) ensures dependent modules are rebuilt in the same reactor
- this avoids false failures caused by `merchantops-api` compiling against stale jars in the local Maven cache

Use the full reactor only when you want the broader baseline:

```powershell
.\mvnw.cmd test
```

## What Is Covered Today

Current automated coverage is focused on the Week 2 tenant user-management loop.

### `merchantops-api` tests

- `AuthSecurityIntegrationTest`
  - real `POST /api/v1/auth/login` success and wrong-password failure paths
  - JWT claim generation and parsing for tenant, role, and permission data
  - real `SecurityConfig` + `JwtAuthenticationFilter` + `RequirePermissionInterceptor` behavior for `GET /api/v1/roles`, `GET /api/v1/users`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, and `PUT /api/v1/users/{id}/roles`
  - `401` when Bearer token is missing or invalid
  - `403` when login succeeds but `USER_READ` is absent
  - `403` when login succeeds but `USER_WRITE` is absent
  - `200` for a valid admin token on `GET /api/v1/roles`, including tenant-only role visibility
  - `200` for a valid read-only user token on `GET /api/v1/users`, including tenant-only user visibility
  - `400` when create requests try to bind role codes outside the current tenant
  - successful create-user flow with BCrypt password persistence and immediate login
  - successful profile-update flow for `PUT /api/v1/users/{id}` with tenant-scoped persistence
  - successful disable-user flow for `PATCH /api/v1/users/{id}/status` followed by login rejection for `DISABLED`
  - rejection of a pre-disable token on protected endpoints after the user becomes `DISABLED`
  - successful role-reassignment flow for `PUT /api/v1/users/{id}/roles`
  - rejection of a pre-change token after role or permission claims become stale
  - successful re-login after role reassignment with new RBAC access
- `UserQueryServiceTest`
  - page defaulting and max-size normalization
  - filter trimming for `username`, `status`, and `roleCode`
  - page result mapping into `UserPageResponse`
  - list and status-filtered list mapping
  - username existence delegation
  - detail lookup success path
  - detail lookup `NOT_FOUND` path
- `UserCommandServiceTest`
  - duplicate username rejection
  - duplicate username rejection with `excludeUserId`
  - role-code rejection when requested roles are not available in the current tenant
  - create-user persistence with `ACTIVE` default status, BCrypt hashing, and `user_role` writes
  - profile update persistence for mutable fields only
  - status update persistence for `ACTIVE` and `DISABLED`
  - invalid status rejection
  - role reassignment with clear-then-write `user_role` semantics
  - role reassignment rejection when requested role codes are not available in the current tenant
  - tenant-scoped missing-user rejection
  - current password-update placeholder returning unified `BIZ_ERROR` rather than an uncaught runtime exception
- `UserManagementControllerTest`
  - HTTP request binding for `page`, `size`, `username`, `status`, and `roleCode`
  - HTTP request binding for `POST /api/v1/users`
  - HTTP request binding for `PUT /api/v1/users/{id}` and `PATCH /api/v1/users/{id}/status`
  - HTTP request binding for `PUT /api/v1/users/{id}/roles`
  - `401` when authentication is missing
  - `403` when `USER_READ` or `USER_WRITE` is missing
  - `401` when authentication exists but tenant context is missing
  - tenant resolution through request-scoped context and forwarding to `UserQueryService`
  - tenant resolution through request-scoped context and forwarding to `UserCommandService`
  - wrapping successful responses with `ApiResponse.success(...)`
- `RoleControllerTest`
  - `GET /api/v1/roles` unauthorized / forbidden / success paths
  - tenant resolution through request-scoped context and forwarding to `RoleQueryService`

### `merchantops-infra` tests

- `UserRepositoryTest`
  - tenant-scoped native page query
  - `username`, `status`, and `roleCode` filtering
  - `DISTINCT` deduplication across joined role rows
  - pagination ordering and count stability

## What Still Needs Manual Verification

These areas are not replaced by the current unit tests:

- authenticated behavior of endpoints outside the covered login + `/api/v1/roles` + `/api/v1/users` (`GET`, `POST`, `PUT`, and `PATCH`) path, such as `/api/v1/user/me`, `/api/v1/context`, and the RBAC demo endpoints
- Swagger/OpenAPI documentation rendering
- real infra health (`MySQL`, `Redis`, `RabbitMQ`)

Use [local-smoke-test.md](local-smoke-test.md) and [regression-checklist.md](regression-checklist.md) for those checks.

## Recommended Workflow

1. Run `.\mvnw.cmd -pl merchantops-api -am test`
2. If that passes, run the manual user-management smoke checks
3. If controller, security, SQL, or docs changed, run the full regression checklist

## Known Pitfalls

- Keep `.\mvnw.cmd -pl merchantops-api -am test` as the default regression entry. Running only `-pl merchantops-api test` can hide sibling-module signature changes behind stale local Maven artifacts.
- For live smoke tests after changing JPA entities, repositories, or API-module dependencies, run `.\mvnw.cmd -pl merchantops-api -am install -DskipTests` first, then start the app from the `merchantops-api` module with `..\mvnw.cmd spring-boot:run`. The `spring-boot:run` classpath resolves sibling modules from the local Maven repository, not from uninstalled reactor outputs.
- Do not treat `merchantops-api/target/merchantops-api-0.0.1-SNAPSHOT.jar` as the default local smoke-test entry point. The current packaging does not produce a fat jar that is ready for `java -jar`.
- For H2-based native SQL tests that rely on `MODE=MySQL`, keep `@AutoConfigureTestDatabase(replace = NONE)` and verify the mode through `INFORMATION_SCHEMA.SETTINGS`. `DatabaseMetaData#getURL()` does not reliably echo the `MODE=...` parameter.
- Treat password edge cases as an explicit regression item. If create-user or login password handling changes, verify that leading and trailing whitespace behavior is consistent across both flows before documenting a final business rule.

## Troubleshooting

- If `-pl merchantops-api test` fails with missing repository methods or stale signatures, rerun with `-am`
- If `spring-boot:run` fails after module-signature changes, install the reactor modules first with `.\mvnw.cmd -pl merchantops-api -am install -DskipTests`
- If automated `/api/v1/users` coverage passes but live verification fails, focus next on runtime config differences such as real JWT secrets, external infra, or deployment-only filters
- If the page contract changes, update both the tests and the user-management docs in the same change
