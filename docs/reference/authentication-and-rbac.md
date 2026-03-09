# Authentication and RBAC

## Implemented Auth Endpoint

- `POST /api/v1/auth/login`

Current behavior:

- verifies `tenantCode`, `username`, and `password`
- requires both tenant and user status to be `ACTIVE`
- returns a JWT `accessToken` when login succeeds

Request example:

```json
{
  "tenantCode": "demo-shop",
  "username": "admin",
  "password": "123456"
}
```

Response `data` example:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9....",
  "tokenType": "Bearer",
  "expiresIn": 7200
}
```

## Protected Endpoints

- `GET /api/v1/user/me`: current user identity, tenant, roles, and permissions
- `GET /api/v1/context`: current tenant and user context from thread-local holders
- `GET /api/v1/users`: returns a summary list of users in the current tenant and requires `USER_READ`

Token usage:

- send `Authorization: Bearer <accessToken>`
- missing or invalid token returns `401 UNAUTHORIZED`
- permission denial returns `403 FORBIDDEN`

Quick check:

```bash
TOKEN=<paste-accessToken-from-login-response>
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/user/me
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/context
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users
```

PowerShell:

```powershell
$token = "<paste-accessToken-from-login-response>"
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/user/me
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/context
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/v1/users
```

## Context Propagation

- `JwtAuthenticationFilter` parses the JWT and writes `TenantContext` and `CurrentUserContext`
- context is cleared in the filter `finally` block after every request
- business code can use `ContextAccess.requireTenantId()` and `ContextAccess.requireUserId()`

## RBAC Demo Endpoints

- `GET /api/v1/rbac/users` requires `USER_READ`
- `GET /api/v1/rbac/users/manage` requires `USER_WRITE`
- `GET /api/v1/rbac/feature-flags` requires `FEATURE_FLAG_MANAGE`

## User Listing Endpoint

- `GET /api/v1/users`
- requires `USER_READ`
- reads `tenantId` from the authenticated request context
- returns only users that belong to the current tenant
- response items use a summary DTO with:
  - `id`
  - `username`
  - `displayName`
  - `email`
  - `status`

Response example:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": [
    {
      "id": 1,
      "username": "admin",
      "displayName": "Admin User",
      "email": "admin@demo-shop.local",
      "status": "ACTIVE"
    }
  ]
}
```

Demo users under tenant `demo-shop`:

- `admin`: full demo permissions from `TENANT_ADMIN`
- `ops`: `USER_READ`, `ORDER_READ`
- `viewer`: `USER_READ`

## Tenant Isolation Note

Role and permission lookup during login is constrained by both `userId` and `tenantId` to reduce the chance of cross-tenant claim pollution when inconsistent link data exists.
