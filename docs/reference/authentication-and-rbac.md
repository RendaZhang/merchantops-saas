# Authentication and RBAC

## Endpoints

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/user/me`
- `GET /api/v1/context`
- `GET /api/v1/roles` (requires `USER_WRITE`; see [user-management.md](user-management.md))
- `GET /api/v1/users` (requires `USER_READ`; see [user-management.md](user-management.md))
- `GET /api/v1/users/{id}` (requires `USER_READ`; see [user-management.md](user-management.md))
- `POST /api/v1/users` (requires `USER_WRITE`; see [user-management.md](user-management.md))
- `PUT /api/v1/users/{id}` (requires `USER_WRITE`; see [user-management.md](user-management.md))
- `PATCH /api/v1/users/{id}/status` (requires `USER_WRITE`; see [user-management.md](user-management.md))
- `PUT /api/v1/users/{id}/roles` (requires `USER_WRITE`; see [user-management.md](user-management.md))
- `POST /api/v1/users/{id}/disable-requests` (requires `USER_WRITE`; see [audit-approval.md](audit-approval.md))
- `GET /api/v1/tickets` (requires `TICKET_READ`; see [ticket-workflow.md](ticket-workflow.md))
- `GET /api/v1/tickets/{id}` (requires `TICKET_READ`; see [ticket-workflow.md](ticket-workflow.md))
- `GET /api/v1/tickets/{id}/ai-interactions` (requires `TICKET_READ`; see [ticket-workflow.md](ticket-workflow.md))
- `POST /api/v1/tickets/{id}/ai-summary` (requires `TICKET_READ`; see [ticket-workflow.md](ticket-workflow.md))
- `POST /api/v1/tickets/{id}/ai-triage` (requires `TICKET_READ`; see [ticket-workflow.md](ticket-workflow.md))
- `POST /api/v1/tickets/{id}/ai-reply-draft` (requires `TICKET_READ`; see [ticket-workflow.md](ticket-workflow.md))
- `GET /api/v1/ai-interactions/usage-summary` (requires `USER_READ`; see [ai-integration.md](ai-integration.md))
- `POST /api/v1/tickets` (requires `TICKET_WRITE`; see [ticket-workflow.md](ticket-workflow.md))
- `PATCH /api/v1/tickets/{id}/assignee` (requires `TICKET_WRITE`; see [ticket-workflow.md](ticket-workflow.md))
- `PATCH /api/v1/tickets/{id}/status` (requires `TICKET_WRITE`; see [ticket-workflow.md](ticket-workflow.md))
- `POST /api/v1/tickets/{id}/comments/proposals/ai-reply-draft` (requires `TICKET_WRITE`; see [ticket-workflow.md](ticket-workflow.md))
- `POST /api/v1/tickets/{id}/comments` (requires `TICKET_WRITE`; see [ticket-workflow.md](ticket-workflow.md))
- `GET /api/v1/audit-events` (requires `USER_READ`; see [audit-approval.md](audit-approval.md))
- `GET /api/v1/approval-requests` (requires action-specific approval read; see [audit-approval.md](audit-approval.md))
- `GET /api/v1/approval-requests/{id}` (requires action-specific approval read; see [audit-approval.md](audit-approval.md))
- `POST /api/v1/approval-requests/{id}/approve` (requires action-specific approval review; see [audit-approval.md](audit-approval.md))
- `POST /api/v1/approval-requests/{id}/reject` (requires action-specific approval review; see [audit-approval.md](audit-approval.md))
- `GET /api/v1/import-jobs` (requires `USER_READ`; see [import-jobs.md](import-jobs.md))
- `GET /api/v1/import-jobs/{id}` (requires `USER_READ`; see [import-jobs.md](import-jobs.md))
- `GET /api/v1/import-jobs/{id}/errors` (requires `USER_READ`; see [import-jobs.md](import-jobs.md))
- `GET /api/v1/import-jobs/{id}/ai-interactions` (requires `USER_READ`; see [import-jobs.md](import-jobs.md))
- `POST /api/v1/import-jobs/{id}/ai-error-summary` (requires `USER_READ`; see [import-jobs.md](import-jobs.md))
- `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion` (requires `USER_READ`; see [import-jobs.md](import-jobs.md))
- `POST /api/v1/import-jobs/{id}/ai-fix-recommendation` (requires `USER_READ`; see [import-jobs.md](import-jobs.md))
- `POST /api/v1/import-jobs` (requires `USER_WRITE`; see [import-jobs.md](import-jobs.md))
- `POST /api/v1/import-jobs/{id}/replay-failures` (requires `USER_WRITE`; see [import-jobs.md](import-jobs.md))
- `POST /api/v1/import-jobs/{id}/replay-file` (requires `USER_WRITE`; see [import-jobs.md](import-jobs.md))
- `POST /api/v1/import-jobs/{id}/replay-failures/selective` (requires `USER_WRITE`; see [import-jobs.md](import-jobs.md))
- `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals` (requires `USER_WRITE`; see [import-jobs.md](import-jobs.md))
- `POST /api/v1/import-jobs/{id}/replay-failures/edited` (requires `USER_WRITE`; see [import-jobs.md](import-jobs.md))
- `GET /api/v1/feature-flags` (requires `FEATURE_FLAG_MANAGE`; see [feature-flags.md](feature-flags.md))
- `PUT /api/v1/feature-flags/{key}` (requires `FEATURE_FLAG_MANAGE`; see [feature-flags.md](feature-flags.md))
- `GET /api/v1/rbac/users` (requires `USER_READ`)
- `GET /api/v1/rbac/users/manage` (requires `USER_WRITE`)

All endpoints above are visible in Swagger UI.

## Demo Tenant and Accounts

Tenant:

- `demo-shop`

Accounts:

- `admin / 123456` (`TENANT_ADMIN`)
- `ops / 123456` (`OPS_USER`)
- `viewer / 123456` (`READ_ONLY`)

Quick-check safety note:

- use a newly created smoke user for update, status, and role-assignment examples instead of mutating the seeded `admin`, `ops`, or `viewer` accounts

## Login Flow

Behavior:

- login is tenant-scoped: `tenantCode + username + password`
- password must not start or end with whitespace
- tenant and user must both be `ACTIVE`
- success creates one server-side `auth_session` row with `ACTIVE` status and `expires_at = now + jwt.expire-seconds`
- success returns `accessToken` with tenant, user, roles, permissions, and a required `sid` session claim
- the public login response shape is unchanged: `accessToken`, `tokenType`, and `expiresIn`
- multiple logins create independent active sessions

Request example:

```json
{
  "tenantCode": "demo-shop",
  "username": "admin",
  "password": "123456"
}
```

Success response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "accessToken": "<jwt-token>",
    "tokenType": "Bearer",
    "expiresIn": 7200
  }
}
```

Credential failure response:

```json
{
  "code": "BAD_REQUEST",
  "message": "username or password is incorrect",
  "data": null
}
```

Password-format failure response:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "password: password must not start or end with whitespace",
  "data": null
}
```

Password handling note:

- login and create-user password behavior must stay consistent
- the current rule rejects leading and trailing whitespace in both flows

## Logout Flow

Endpoint:

- `POST /api/v1/auth/logout`

Behavior:

- requires `Authorization: Bearer <accessToken>`
- revokes only the current `sid` session by setting `status=REVOKED` and `revoked_at`
- does not revoke other active sessions for the same user
- returns `200` with the standard success envelope and `data: null`
- reusing the same token after logout returns `401 UNAUTHORIZED` on protected endpoints such as `/api/v1/context`
- stale tenant, user, role, or permission state follows the same protected-request `403 FORBIDDEN` rules as other authenticated endpoints

Success response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": null
}
```

Deferred lifecycle scope:

- there is no refresh token in this slice
- there is no cookie/session rotation in this slice
- there is no logout-all-devices flow in this slice
- there is no session cleanup scheduler in this slice
- after the access token expires, the user must log in again

## Bearer Token Usage

- send `Authorization: Bearer <accessToken>`
- missing/invalid token returns `401 UNAUTHORIZED`
- JWTs must carry a non-blank `sid` claim; sidless beta tokens issued before this boundary change are invalid and return `401 UNAUTHORIZED`
- protected requests validate the server-side session exists, belongs to the same tenant and user, is `ACTIVE`, is not revoked, and has `expires_at > now` before setting Spring authentication
- missing, revoked, expired, mismatched, or sidless sessions return `401 UNAUTHORIZED` with message `authentication required`
- missing required permission returns `403 FORBIDDEN`
- protected requests re-check the current tenant row, so a token issued before the tenant was disabled is rejected with `403 tenant is not active`
- protected requests re-check the current user row, so a token issued before a user was disabled is rejected with `403 user is not active`
- protected requests also re-check current roles and permissions, so a token issued before role or permission changes is rejected with `403 token claims are stale, please login again`
- the same stale-claim rule applies to current public import read endpoints, tenant AI usage-summary, and suggestion-only AI endpoints that authorize from current `USER_READ`
- the shared approval surface is action-aware: `USER_STATUS_DISABLE` and `IMPORT_JOB_SELECTIVE_REPLAY` use `USER_READ` / `USER_WRITE`, while `TICKET_COMMENT_CREATE` uses `TICKET_READ` / `TICKET_WRITE`
- run [../runbooks/automated-tests.md](../runbooks/automated-tests.md) before manual RBAC smoke checks when code changed

Quick check:

```bash
TOKEN=<paste-accessToken-from-login-response>
SMOKE_USERNAME="cashier-$(date +%s)"
SMOKE_EMAIL="${SMOKE_USERNAME}@demo-shop.local"
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/user/me
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/context
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/roles
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/tickets?page=0&size=10"
curl -i -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"username\":\"$SMOKE_USERNAME\",\"displayName\":\"Cashier User\",\"email\":\"$SMOKE_EMAIL\",\"password\":\"123456\",\"roleCodes\":[\"READ_ONLY\"]}" \
  http://localhost:8080/api/v1/users
NEW_USER_ID=<paste-id-from-create-response>
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users/$NEW_USER_ID
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"tenantCode\":\"demo-shop\",\"username\":\"$SMOKE_USERNAME\",\"password\":\"123456\"}"
SMOKE_TOKEN=<paste-accessToken-from-smoke-user-login-response>
curl -i -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"displayName\":\"Updated Cashier\",\"email\":\"$SMOKE_USERNAME.updated@demo-shop.local\"}" \
  http://localhost:8080/api/v1/users/$NEW_USER_ID
curl -i -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"roleCodes":["TENANT_ADMIN"]}' \
  http://localhost:8080/api/v1/users/$NEW_USER_ID/roles
curl -i -H "Authorization: Bearer $SMOKE_TOKEN" http://localhost:8080/api/v1/context
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"tenantCode\":\"demo-shop\",\"username\":\"$SMOKE_USERNAME\",\"password\":\"123456\"}"
REFRESHED_TOKEN=<paste-accessToken-from-role-refresh-login-response>
curl -i -H "Authorization: Bearer $REFRESHED_TOKEN" http://localhost:8080/api/v1/feature-flags
curl -i -X PATCH -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"status":"DISABLED"}' \
  http://localhost:8080/api/v1/users/$NEW_USER_ID/status
curl -i -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"title":"POS register frozen","description":"Register screen froze during checkout."}' \
  http://localhost:8080/api/v1/tickets
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"tenantCode\":\"demo-shop\",\"username\":\"$SMOKE_USERNAME\",\"password\":\"123456\"}"
curl -i -H "Authorization: Bearer $REFRESHED_TOKEN" http://localhost:8080/api/v1/context
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"tenantCode\":\"demo-shop\",\"username\":\"admin\",\"password\":\"123456\"}"
LOGOUT_TOKEN=<paste-accessToken-from-logout-check-login-response>
curl -i -X POST -H "Authorization: Bearer $LOGOUT_TOKEN" http://localhost:8080/api/v1/auth/logout
curl -i -H "Authorization: Bearer $LOGOUT_TOKEN" http://localhost:8080/api/v1/context
```

PowerShell:

```powershell
$token = "<paste-accessToken-from-login-response>"
$smokeUsername = "cashier-{0}" -f [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$smokeEmail = "$smokeUsername@demo-shop.local"
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/user/me
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/context
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/roles
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/users
curl.exe -i -H "Authorization: Bearer $token" "http://localhost:8080/api/v1/tickets?page=0&size=10"
$createBody = @{ username = $smokeUsername; displayName = "Cashier User"; email = $smokeEmail; password = "123456"; roleCodes = @("READ_ONLY") } | ConvertTo-Json -Compress
curl.exe -i -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d $createBody http://localhost:8080/api/v1/users
$newUserId = "<paste-id-from-create-response>"
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/users/$newUserId
curl.exe -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"tenantCode\":\"demo-shop\",\"username\":\"$smokeUsername\",\"password\":\"123456\"}"
$smokeToken = "<paste-accessToken-from-smoke-user-login-response>"
$updateBody = @{ displayName = "Updated Cashier"; email = "$smokeUsername.updated@demo-shop.local" } | ConvertTo-Json -Compress
curl.exe -i -X PUT -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d $updateBody http://localhost:8080/api/v1/users/$newUserId
curl.exe -i -X PUT -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "{\"roleCodes\":[\"TENANT_ADMIN\"]}" http://localhost:8080/api/v1/users/$newUserId/roles
curl.exe -i -H "Authorization: Bearer $smokeToken" http://localhost:8080/api/v1/context
curl.exe -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"tenantCode\":\"demo-shop\",\"username\":\"$smokeUsername\",\"password\":\"123456\"}"
$refreshedToken = "<paste-accessToken-from-role-refresh-login-response>"
curl.exe -i -H "Authorization: Bearer $refreshedToken" http://localhost:8080/api/v1/feature-flags
curl.exe -i -X PATCH -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "{\"status\":\"DISABLED\"}" http://localhost:8080/api/v1/users/$newUserId/status
$ticketBody = @{ title = "POS register frozen"; description = "Register screen froze during checkout." } | ConvertTo-Json -Compress
curl.exe -i -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d $ticketBody http://localhost:8080/api/v1/tickets
curl.exe -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"tenantCode\":\"demo-shop\",\"username\":\"$smokeUsername\",\"password\":\"123456\"}"
curl.exe -i -H "Authorization: Bearer $refreshedToken" http://localhost:8080/api/v1/context
curl.exe -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"tenantCode\":\"demo-shop\",\"username\":\"admin\",\"password\":\"123456\"}"
$logoutToken = "<paste-accessToken-from-logout-check-login-response>"
curl.exe -i -X POST -H "Authorization: Bearer $logoutToken" http://localhost:8080/api/v1/auth/logout
curl.exe -i -H "Authorization: Bearer $logoutToken" http://localhost:8080/api/v1/context
```

## Core Response Examples

### `GET /api/v1/user/me`

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "userId": 1,
    "tenantId": 1,
    "tenantCode": "demo-shop",
    "username": "admin",
    "roles": ["TENANT_ADMIN"],
    "permissions": ["USER_READ", "USER_WRITE", "ORDER_READ", "BILLING_READ", "FEATURE_FLAG_MANAGE", "TICKET_READ", "TICKET_WRITE"]
  }
}
```

### `GET /api/v1/context`

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "tenantId": 1,
    "tenantCode": "demo-shop",
    "userId": 1,
    "username": "admin"
  }
}
```

### `GET /api/v1/users`

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "items": [
      {
        "id": 1,
        "username": "admin",
        "displayName": "Demo Admin",
        "email": "admin@demo-shop.local",
        "status": "ACTIVE"
      }
    ],
    "page": 0,
    "size": 10,
    "total": 1,
    "totalPages": 1
  }
}
```

Current notes:

- tenant scope is derived from JWT/request context, not from request parameters
- supported filters are `username` (fuzzy), `status` (exact), and `roleCode` (exact)
- supported pagination parameters are `page` and `size`
- use [user-management.md](user-management.md) for the current user-management boundary and validation references

### `GET /api/v1/users/{id}`

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "id": 3,
    "tenantId": 1,
    "username": "viewer",
    "displayName": "Viewer User",
    "email": "viewer@demo-shop.local",
    "status": "ACTIVE",
    "roleCodes": ["READ_ONLY"],
    "createdAt": "2026-03-10T10:00:00",
    "updatedAt": "2026-03-10T10:30:00"
  }
}
```

Current notes:

- requires `USER_READ`
- returns only current-tenant users
- includes current `roleCodes` so detail and role-management screens can share one read path

### `GET /api/v1/roles`

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "items": [
      {
        "id": 11,
        "roleCode": "TENANT_ADMIN",
        "roleName": "Tenant Admin"
      },
      {
        "id": 13,
        "roleCode": "READ_ONLY",
        "roleName": "Read Only User"
      }
    ]
  }
}
```

Current notes:

- requires `USER_WRITE`
- returns current-tenant roles that are valid for `PUT /api/v1/users/{id}/roles`
- this endpoint is intentionally scoped to the assignment workflow rather than the read-only user list workflow

### `POST /api/v1/users`

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "id": 5,
    "tenantId": 1,
    "username": "cashier",
    "displayName": "Cashier User",
    "email": "cashier@demo-shop.local",
    "status": "ACTIVE",
    "roleCodes": ["READ_ONLY"],
    "createdAt": "2026-03-10T11:00:00",
    "updatedAt": "2026-03-10T11:00:00"
  }
}
```

Current notes:

- tenant scope is derived from JWT/request context, not from request body
- requires `USER_WRITE`
- `admin` can create users with tenant-local role codes
- `ops` and `viewer` are denied because they do not hold `USER_WRITE` in the seeded data
- passwords are stored as BCrypt and can be used immediately for login
- role codes outside the current tenant return `BAD_REQUEST`
- the database also enforces that every `user_role` row has the same `tenant_id` as both its `users` row and its `role` row; direct cross-tenant role bindings fail below the service layer

### `PUT /api/v1/users/{id}`

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "id": 5,
    "tenantId": 1,
    "username": "cashier",
    "displayName": "Updated Cashier",
    "email": "cashier.updated@demo-shop.local",
    "status": "ACTIVE",
    "updatedAt": "2026-03-10T14:00:00"
  }
}
```

Current notes:

- requires `USER_WRITE`
- only `displayName` and `email` are mutable
- `tenantId`, `username`, and `passwordHash` are not part of the public request contract

### `PATCH /api/v1/users/{id}/status`

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "id": 5,
    "tenantId": 1,
    "username": "cashier",
    "displayName": "Updated Cashier",
    "email": "cashier.updated@demo-shop.local",
    "status": "DISABLED",
    "updatedAt": "2026-03-10T14:10:00"
  }
}
```

Current notes:

- requires `USER_WRITE`
- only `ACTIVE` and `DISABLED` are allowed
- disabled users are rejected by login because login requires `ACTIVE`
- tokens issued before the disable operation are also rejected on protected endpoints because request authentication re-checks the current user status

### `PUT /api/v1/users/{id}/roles`

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "id": 3,
    "tenantId": 1,
    "username": "viewer",
    "roleCodes": ["TENANT_ADMIN"],
    "permissionCodes": ["USER_READ", "USER_WRITE", "ORDER_READ", "BILLING_READ", "FEATURE_FLAG_MANAGE", "TICKET_READ", "TICKET_WRITE"],
    "updatedAt": "2026-03-10T18:00:00"
  }
}
```

Current notes:

- requires `USER_WRITE`
- reassigns roles by clearing old `user_role` rows first, then writing the new role set
- only role codes from the current tenant are allowed
- new `user_role` bindings are written with the current tenant id, and database-level composite foreign keys enforce that the bound user and role belong to that same tenant
- tokens issued before the role or permission change are rejected on protected endpoints with `token claims are stale, please login again`
- the affected user must login again so the new token carries the new roles and permissions

### `GET /api/v1/rbac/users/manage` (with `viewer` token)

```json
{
  "code": "FORBIDDEN",
  "message": "permission denied",
  "data": null
}
```

### `GET /api/v1/feature-flags`

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "items": [
      {
        "id": 4,
        "key": "ai.import.error-summary.enabled",
        "enabled": true,
        "updatedAt": "2026-04-06T13:18:00"
      },
      {
        "id": 1,
        "key": "ai.ticket.summary.enabled",
        "enabled": true,
        "updatedAt": "2026-04-06T13:18:00"
      }
    ]
  }
}
```

Current notes:

- requires `FEATURE_FLAG_MANAGE`
- returns the current tenant's persisted feature flags in stable `key ASC` order
- each item exposes `id`, `key`, `enabled`, and `updatedAt`
- the current fixed flag set covers six AI generation endpoints plus the two Week 8 workflow proposal bridges
- `merchantops.ai.enabled` stays config-only and is not managed through this API
- `PUT /api/v1/feature-flags/{key}` accepts `{ "enabled": true|false }`, rejects `enabled=null` with `400 BAD_REQUEST`, returns `404` for an unknown key, and is idempotent when the requested state is unchanged

### `GET /api/v1/tickets`

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "items": [
      {
        "id": 302,
        "title": "Printer cable replacement",
        "status": "IN_PROGRESS",
        "assigneeId": 2,
        "assigneeUsername": "ops",
        "createdAt": "2026-03-11T09:30:00",
        "updatedAt": "2026-03-11T10:15:00"
      }
    ],
    "page": 0,
    "size": 10,
    "total": 1,
    "totalPages": 1
  }
}
```

Current notes:

- requires `TICKET_READ`
- seeded `admin`, `ops`, and `viewer` can read ticket queues in their own tenant
- use [ticket-workflow.md](ticket-workflow.md) for the current list/detail and close-loop rules

## Context Propagation

- `JwtAuthenticationFilter` parses JWT, re-checks the current tenant status, user status, roles, and permissions from the database, and writes `TenantContext` and `CurrentUserContext`
- before tenant/user/role revalidation, the filter requires an active matching `auth_session` for the JWT `sid`
- context is cleared in `finally` for every request
- business code reads context through `ContextAccess`

## Expected RBAC Behavior

- any authenticated active session can call `POST /api/v1/auth/logout` for its own session
- `admin` can access `GET /api/v1/roles`, `GET /api/v1/users`, `GET /api/v1/users/{id}`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, `PUT /api/v1/users/{id}/roles`, `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}`, `GET /api/v1/tickets/{id}/ai-interactions`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, `POST /api/v1/tickets/{id}/ai-reply-draft`, `GET /api/v1/ai-interactions/usage-summary`, `POST /api/v1/tickets`, `PATCH /api/v1/tickets/{id}/assignee`, `PATCH /api/v1/tickets/{id}/status`, `POST /api/v1/tickets/{id}/comments`, `GET /api/v1/import-jobs`, `GET /api/v1/import-jobs/{id}`, `GET /api/v1/import-jobs/{id}/errors`, `GET /api/v1/import-jobs/{id}/ai-interactions`, `POST /api/v1/import-jobs/{id}/ai-error-summary`, `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`, `POST /api/v1/import-jobs/{id}/ai-fix-recommendation`, `POST /api/v1/import-jobs`, `POST /api/v1/import-jobs/{id}/replay-failures`, `POST /api/v1/import-jobs/{id}/replay-file`, `POST /api/v1/import-jobs/{id}/replay-failures/selective`, `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals`, `POST /api/v1/import-jobs/{id}/replay-failures/edited`, `GET /api/v1/feature-flags`, `PUT /api/v1/feature-flags/{key}`, `/api/v1/rbac/users`, and `/api/v1/rbac/users/manage`
- `ops` can access `GET /api/v1/users`, `GET /api/v1/users/{id}`, `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}`, `GET /api/v1/tickets/{id}/ai-interactions`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, `POST /api/v1/tickets/{id}/ai-reply-draft`, `GET /api/v1/ai-interactions/usage-summary`, `POST /api/v1/tickets`, `PATCH /api/v1/tickets/{id}/assignee`, `PATCH /api/v1/tickets/{id}/status`, `POST /api/v1/tickets/{id}/comments`, `GET /api/v1/import-jobs`, `GET /api/v1/import-jobs/{id}`, `GET /api/v1/import-jobs/{id}/errors`, `GET /api/v1/import-jobs/{id}/ai-interactions`, `POST /api/v1/import-jobs/{id}/ai-error-summary`, `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`, `POST /api/v1/import-jobs/{id}/ai-fix-recommendation`, and `/api/v1/rbac/users`
- `viewer` can access `GET /api/v1/users`, `GET /api/v1/users/{id}`, `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}`, `GET /api/v1/tickets/{id}/ai-interactions`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, `POST /api/v1/tickets/{id}/ai-reply-draft`, `GET /api/v1/ai-interactions/usage-summary`, `GET /api/v1/import-jobs`, `GET /api/v1/import-jobs/{id}`, `GET /api/v1/import-jobs/{id}/errors`, `GET /api/v1/import-jobs/{id}/ai-interactions`, `POST /api/v1/import-jobs/{id}/ai-error-summary`, `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`, `POST /api/v1/import-jobs/{id}/ai-fix-recommendation`, and `/api/v1/rbac/users`
- `ops` and `viewer` are denied on `GET /api/v1/roles`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, `PUT /api/v1/users/{id}/roles`, `POST /api/v1/import-jobs`, `POST /api/v1/import-jobs/{id}/replay-failures`, `POST /api/v1/import-jobs/{id}/replay-file`, `POST /api/v1/import-jobs/{id}/replay-failures/selective`, `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals`, and `POST /api/v1/import-jobs/{id}/replay-failures/edited`
- `viewer` is denied on `POST /api/v1/tickets`, `PATCH /api/v1/tickets/{id}/assignee`, `PATCH /api/v1/tickets/{id}/status`, and `POST /api/v1/tickets/{id}/comments`
- `ops` and `viewer` are denied on endpoints requiring permissions they do not have
- after `viewer` is promoted and logs in again, the refreshed token can access `/api/v1/rbac/users/manage`, `/api/v1/feature-flags`, and ticket write endpoints

Approval routing notes:

- `GET /api/v1/approval-requests` and `GET /api/v1/approval-requests/{id}` are filtered by action-specific read capability rather than a controller-wide `USER_READ` gate
- `POST /api/v1/approval-requests/{id}/approve` and `POST /api/v1/approval-requests/{id}/reject` are filtered by action-specific review capability rather than a controller-wide `USER_WRITE` gate
- `admin` can read and review all three current approval action types because the role carries both `USER_*` and `TICKET_*`
- `ops` can create ticket comment proposals and can review `TICKET_COMMENT_CREATE` requests because the role carries `TICKET_WRITE`, but still cannot review `USER_STATUS_DISABLE` or `IMPORT_JOB_SELECTIVE_REPLAY` without `USER_WRITE`
- `viewer` can read only the approval action types exposed by its read permissions and cannot approve or reject any approval request

The automated suite now covers the login -> server-side session -> JWT -> `/api/v1/context`, `/api/v1/auth/logout`, `/api/v1/users` (`GET`, `GET /{id}`, `POST`, `PUT`, `PATCH`, and `PUT /api/v1/users/{id}/roles`), `/api/v1/feature-flags` (`GET` and `PUT`), `/api/v1/tickets` (`GET`, `GET /{id}`, `POST`, `PATCH /assignee`, `PATCH /status`, `POST /comments`, and `POST /comments/proposals/ai-reply-draft`), `/api/v1/ai-interactions/usage-summary`, the current import read / AI read surface (`GET /api/v1/import-jobs`, `GET /api/v1/import-jobs/{id}`, `GET /api/v1/import-jobs/{id}/errors`, `GET /api/v1/import-jobs/{id}/ai-interactions`, `POST /api/v1/import-jobs/{id}/ai-error-summary`, `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`, and `POST /api/v1/import-jobs/{id}/ai-fix-recommendation`), plus the Week 8 import and ticket proposal/approval paths (`POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals`, `POST /api/v1/tickets/{id}/comments/proposals/ai-reply-draft`, and the shared approval review endpoints) permission paths end to end, including active auth-session creation, required `sid` parsing, sidless token rejection, revoked-session rejection, expired-session rejection, independent multi-session behavior, inactive-tenant rejection, disabled-user rejection, stale-claim rejection, database-level rejection of cross-tenant `user_role` bindings, database-level rejection of cross-tenant root ticket assignee/creator bindings, mixed-action approval visibility, ticket status-transition rejection, import-job tenant isolation, tenant AI usage-summary isolation, approval self-review rejection, feature-flag `403` and stale-claim re-login coverage, and re-login with refreshed permissions. Manual permission verification is still necessary for `/api/v1/user/me`, Swagger authorization behavior, and the remaining RBAC demo endpoints.

## Current Public RBAC Boundary

- Swagger currently exposes `GET /api/v1/users`, `GET /api/v1/users/{id}`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, and `PUT /api/v1/users/{id}/roles` under the `User Management` tag
- Swagger exposes `POST /api/v1/auth/login` and `POST /api/v1/auth/logout` under the `Authentication` tag
- Swagger exposes `GET /api/v1/roles` under the `Role Management` tag
- Swagger exposes `GET /api/v1/feature-flags` and `PUT /api/v1/feature-flags/{key}` under the `Feature Flags` tag
- Swagger exposes `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}`, `GET /api/v1/tickets/{id}/ai-interactions`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, `POST /api/v1/tickets/{id}/ai-reply-draft`, `POST /api/v1/tickets`, `PATCH /api/v1/tickets/{id}/assignee`, `PATCH /api/v1/tickets/{id}/status`, `POST /api/v1/tickets/{id}/comments/proposals/ai-reply-draft`, and `POST /api/v1/tickets/{id}/comments` under the `Ticket Workflow` tag
- Swagger exposes `GET /api/v1/ai-interactions/usage-summary` under the `AI Governance` tag
- Swagger exposes `GET /api/v1/approval-requests`, `GET /api/v1/approval-requests/{id}`, `POST /api/v1/approval-requests/{id}/approve`, and `POST /api/v1/approval-requests/{id}/reject` under the `Approval Requests` tag with action-aware permission routing
- Swagger exposes `GET /api/v1/import-jobs`, `GET /api/v1/import-jobs/{id}`, `GET /api/v1/import-jobs/{id}/errors`, `GET /api/v1/import-jobs/{id}/ai-interactions`, `POST /api/v1/import-jobs/{id}/ai-error-summary`, `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`, `POST /api/v1/import-jobs/{id}/ai-fix-recommendation`, `POST /api/v1/import-jobs`, `POST /api/v1/import-jobs/{id}/replay-failures`, `POST /api/v1/import-jobs/{id}/replay-file`, `POST /api/v1/import-jobs/{id}/replay-failures/selective`, `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals`, and `POST /api/v1/import-jobs/{id}/replay-failures/edited` under the `Import Jobs` tag
- `GET /api/v1/users` is the formal paged tenant query endpoint
- `GET /api/v1/users/{id}` is the formal tenant-scoped detail endpoint
- `POST /api/v1/users` is the current public create endpoint
- `PUT /api/v1/users/{id}` is the current public profile update endpoint
- `PATCH /api/v1/users/{id}/status` is the current public status-management endpoint
- `PUT /api/v1/users/{id}/roles` is the current public role-assignment endpoint
- `GET /api/v1/feature-flags` and `PUT /api/v1/feature-flags/{key}` are the current public tenant-scoped feature-flag management endpoints
- `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}`, and `GET /api/v1/tickets/{id}/ai-interactions` are the current ticket read endpoints
- `GET /api/v1/ai-interactions/usage-summary` is the current tenant AI governance read endpoint
- `POST /api/v1/tickets/{id}/comments/proposals/ai-reply-draft` is the current ticket proposal-write endpoint for human-reviewed reply-draft execution
- `GET /api/v1/import-jobs`, `GET /api/v1/import-jobs/{id}`, `GET /api/v1/import-jobs/{id}/errors`, `GET /api/v1/import-jobs/{id}/ai-interactions`, `POST /api/v1/import-jobs/{id}/ai-error-summary`, `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`, and `POST /api/v1/import-jobs/{id}/ai-fix-recommendation` are the current import read endpoints
- `POST /api/v1/import-jobs`, `POST /api/v1/import-jobs/{id}/replay-failures`, `POST /api/v1/import-jobs/{id}/replay-file`, `POST /api/v1/import-jobs/{id}/replay-failures/selective`, `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals`, and `POST /api/v1/import-jobs/{id}/replay-failures/edited` are the current import write endpoints
- `POST /api/v1/tickets`, `PATCH /api/v1/tickets/{id}/assignee`, `PATCH /api/v1/tickets/{id}/status`, and `POST /api/v1/tickets/{id}/comments` are the current ticket write endpoints
- the approval queue/detail/approve/reject endpoints are mixed-action public endpoints whose visibility depends on the current action type rather than a single fixed permission
- Treat the Swagger-visible endpoints in this document as the only public API surface

## Tenant Isolation Note

Role and permission lookup during login and protected-request revalidation is constrained by `userId`, `tenantId`, and `user_role.tenant_id`. Since `V16`, the `user_role` table also carries database-level same-tenant composite foreign keys to both `users` and `role`, so direct cross-tenant role bindings are rejected before they can affect JWT claims.
