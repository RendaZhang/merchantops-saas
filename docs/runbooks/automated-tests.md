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
  - real `SecurityConfig` + `JwtAuthenticationFilter` + `RequirePermissionInterceptor` behavior for `GET /api/v1/users`
  - `401` when Bearer token is missing or invalid
  - `403` when login succeeds but `USER_READ` is absent
  - `200` for a valid read-only user token, including tenant-only user visibility
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
  - tenant-scoped missing-user rejection
  - current placeholder write flows returning unified `BIZ_ERROR` rather than uncaught runtime exceptions
- `UserManagementControllerTest`
  - HTTP request binding for `page`, `size`, `username`, `status`, and `roleCode`
  - `401` when authentication is missing
  - `403` when `USER_READ` is missing
  - `401` when authentication exists but tenant context is missing
  - tenant resolution through request-scoped context and forwarding to `UserQueryService`
  - wrapping successful responses with `ApiResponse.success(...)`

### `merchantops-infra` tests

- `UserRepositoryTest`
  - tenant-scoped native page query
  - `username`, `status`, and `roleCode` filtering
  - `DISTINCT` deduplication across joined role rows
  - pagination ordering and count stability

## What Still Needs Manual Verification

These areas are not replaced by the current unit tests:

- authenticated behavior of endpoints outside the covered login + `/api/v1/users` path, such as `/api/v1/user/me`, `/api/v1/context`, and the RBAC demo endpoints
- Swagger/OpenAPI documentation rendering
- real infra health (`MySQL`, `Redis`, `RabbitMQ`)

Use [local-smoke-test.md](local-smoke-test.md) and [regression-checklist.md](regression-checklist.md) for those checks.

## Recommended Workflow

1. Run `.\mvnw.cmd -pl merchantops-api -am test`
2. If that passes, run the manual user-management smoke checks
3. If controller, security, SQL, or docs changed, run the full regression checklist

## Troubleshooting

- If `-pl merchantops-api test` fails with missing repository methods or stale signatures, rerun with `-am`
- If automated `/api/v1/users` coverage passes but live verification fails, focus next on runtime config differences such as real JWT secrets, external infra, or deployment-only filters
- If the page contract changes, update both the tests and the user-management docs in the same change
