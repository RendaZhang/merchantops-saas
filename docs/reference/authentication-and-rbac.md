# Authentication and RBAC

## Endpoints

- `POST /api/v1/auth/login`
- `GET /api/v1/user/me`
- `GET /api/v1/context`
- `GET /api/v1/roles` (requires `USER_WRITE`; see [user-management.md](user-management.md))
- `GET /api/v1/users` (requires `USER_READ`; see [user-management.md](user-management.md))
- `GET /api/v1/users/{id}` (requires `USER_READ`; see [user-management.md](user-management.md))
- `POST /api/v1/users` (requires `USER_WRITE`; see [user-management.md](user-management.md))
- `PUT /api/v1/users/{id}` (requires `USER_WRITE`; see [user-management.md](user-management.md))
- `PATCH /api/v1/users/{id}/status` (requires `USER_WRITE`; see [user-management.md](user-management.md))
- `PUT /api/v1/users/{id}/roles` (requires `USER_WRITE`; see [user-management.md](user-management.md))
- `GET /api/v1/tickets` (requires `TICKET_READ`; see [ticket-workflow.md](ticket-workflow.md))
- `GET /api/v1/tickets/{id}` (requires `TICKET_READ`; see [ticket-workflow.md](ticket-workflow.md))
- `POST /api/v1/tickets/{id}/ai-summary` (requires `TICKET_READ`; see [ticket-workflow.md](ticket-workflow.md))
- `POST /api/v1/tickets/{id}/ai-triage` (requires `TICKET_READ`; see [ticket-workflow.md](ticket-workflow.md))
- `POST /api/v1/tickets` (requires `TICKET_WRITE`; see [ticket-workflow.md](ticket-workflow.md))
- `PATCH /api/v1/tickets/{id}/assignee` (requires `TICKET_WRITE`; see [ticket-workflow.md](ticket-workflow.md))
- `PATCH /api/v1/tickets/{id}/status` (requires `TICKET_WRITE`; see [ticket-workflow.md](ticket-workflow.md))
- `POST /api/v1/tickets/{id}/comments` (requires `TICKET_WRITE`; see [ticket-workflow.md](ticket-workflow.md))
- `GET /api/v1/rbac/users` (requires `USER_READ`)
- `GET /api/v1/rbac/users/manage` (requires `USER_WRITE`)
- `GET /api/v1/rbac/feature-flags` (requires `FEATURE_FLAG_MANAGE`)

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
- success returns `accessToken` with tenant, user, roles, and permissions claims

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

## Bearer Token Usage

- send `Authorization: Bearer <accessToken>`
- missing/invalid token returns `401 UNAUTHORIZED`
- missing required permission returns `403 FORBIDDEN`
- protected requests re-check the current user row, so a token issued before a user was disabled is rejected with `403 user is not active`
- protected requests also re-check current roles and permissions, so a token issued before role or permission changes is rejected with `403 token claims are stale, please login again`
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
curl -i -H "Authorization: Bearer $REFRESHED_TOKEN" http://localhost:8080/api/v1/rbac/users/manage
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
curl.exe -i -H "Authorization: Bearer $refreshedToken" http://localhost:8080/api/v1/rbac/users/manage
curl.exe -i -X PATCH -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "{\"status\":\"DISABLED\"}" http://localhost:8080/api/v1/users/$newUserId/status
$ticketBody = @{ title = "POS register frozen"; description = "Register screen froze during checkout." } | ConvertTo-Json -Compress
curl.exe -i -X POST -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d $ticketBody http://localhost:8080/api/v1/tickets
curl.exe -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"tenantCode\":\"demo-shop\",\"username\":\"$smokeUsername\",\"password\":\"123456\"}"
curl.exe -i -H "Authorization: Bearer $refreshedToken" http://localhost:8080/api/v1/context
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

- `JwtAuthenticationFilter` parses JWT, re-checks the current user status, roles, and permissions from the database, and writes `TenantContext` and `CurrentUserContext`
- context is cleared in `finally` for every request
- business code reads context through `ContextAccess`

## Expected RBAC Behavior

- `admin` can access `GET /api/v1/roles`, `GET /api/v1/users`, `GET /api/v1/users/{id}`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, `PUT /api/v1/users/{id}/roles`, `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, `POST /api/v1/tickets`, `PATCH /api/v1/tickets/{id}/assignee`, `PATCH /api/v1/tickets/{id}/status`, `POST /api/v1/tickets/{id}/comments`, `/api/v1/rbac/users`, `/api/v1/rbac/users/manage`, and `/api/v1/rbac/feature-flags`
- `ops` can access `GET /api/v1/users`, `GET /api/v1/users/{id}`, `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, `POST /api/v1/tickets`, `PATCH /api/v1/tickets/{id}/assignee`, `PATCH /api/v1/tickets/{id}/status`, `POST /api/v1/tickets/{id}/comments`, and `/api/v1/rbac/users`
- `viewer` can access `GET /api/v1/users`, `GET /api/v1/users/{id}`, `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, and `/api/v1/rbac/users`
- `ops` and `viewer` are denied on `GET /api/v1/roles`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, and `PUT /api/v1/users/{id}/roles`
- `viewer` is denied on `POST /api/v1/tickets`, `PATCH /api/v1/tickets/{id}/assignee`, `PATCH /api/v1/tickets/{id}/status`, and `POST /api/v1/tickets/{id}/comments`
- `ops` and `viewer` are denied on endpoints requiring permissions they do not have
- after `viewer` is promoted and logs in again, the refreshed token can access `/api/v1/rbac/users/manage`, `/api/v1/rbac/feature-flags`, and ticket write endpoints

The automated suite now covers the login -> JWT -> `/api/v1/users` (`GET`, `GET /{id}`, `POST`, `PUT`, `PATCH`, and `PUT /api/v1/users/{id}/roles`) and `/api/v1/tickets` (`GET`, `GET /{id}`, `POST`, `PATCH /assignee`, `PATCH /status`, and `POST /comments`) permission paths end to end, including disabled-user rejection, stale-claim rejection, ticket status-transition rejection, and re-login with refreshed permissions. Manual permission verification is still necessary for `/api/v1/user/me`, `/api/v1/context`, Swagger authorization behavior, and the RBAC demo endpoints.

## Current User Management Boundary

- Swagger currently exposes `GET /api/v1/users`, `GET /api/v1/users/{id}`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, and `PUT /api/v1/users/{id}/roles` under the `User Management` tag
- Swagger exposes `GET /api/v1/roles` under the `Role Management` tag
- Swagger exposes `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, `POST /api/v1/tickets`, `PATCH /api/v1/tickets/{id}/assignee`, `PATCH /api/v1/tickets/{id}/status`, and `POST /api/v1/tickets/{id}/comments` under the `Ticket Workflow` tag
- `GET /api/v1/users` is the formal paged tenant query endpoint
- `GET /api/v1/users/{id}` is the formal tenant-scoped detail endpoint
- `POST /api/v1/users` is the current public create endpoint
- `PUT /api/v1/users/{id}` is the current public profile update endpoint
- `PATCH /api/v1/users/{id}/status` is the current public status-management endpoint
- `PUT /api/v1/users/{id}/roles` is the current public role-assignment endpoint
- `GET /api/v1/tickets` and `GET /api/v1/tickets/{id}` are the current ticket read endpoints
- `POST /api/v1/tickets`, `PATCH /api/v1/tickets/{id}/assignee`, `PATCH /api/v1/tickets/{id}/status`, and `POST /api/v1/tickets/{id}/comments` are the current ticket write endpoints
- Treat the Swagger-visible endpoints in this document as the only public API surface

## Tenant Isolation Note

Role and permission lookup during login is constrained by both `userId` and `tenantId` to reduce cross-tenant claim pollution risk when inconsistent link data exists.
