# Authentication and RBAC

## Endpoints

- `POST /api/v1/auth/login`
- `GET /api/v1/user/me`
- `GET /api/v1/context`
- `GET /api/v1/users` (requires `USER_READ`; see [user-management.md](user-management.md))
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

## Login Flow

Behavior:

- login is tenant-scoped: `tenantCode + username + password`
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

## Bearer Token Usage

- send `Authorization: Bearer <accessToken>`
- missing/invalid token returns `401 UNAUTHORIZED`
- missing required permission returns `403 FORBIDDEN`

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
    "permissions": ["USER_READ", "USER_WRITE", "ORDER_READ", "BILLING_READ", "FEATURE_FLAG_MANAGE"]
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
  "data": [
    {
      "id": 1,
      "username": "admin",
      "displayName": "Demo Admin",
      "email": "admin@demo-shop.local",
      "status": "ACTIVE"
    },
    {
      "id": 2,
      "username": "ops",
      "displayName": "Ops User",
      "email": "ops@demo-shop.local",
      "status": "ACTIVE"
    },
    {
      "id": 3,
      "username": "viewer",
      "displayName": "Viewer User",
      "email": "viewer@demo-shop.local",
      "status": "ACTIVE"
    }
  ]
}
```

Current notes:

- tenant scope is derived from JWT/request context, not from request parameters
- the current Swagger contract exposes no `page`, `size`, or `status` parameters yet
- use [user-management.md](user-management.md) for the current user-management boundary and validation references

### `GET /api/v1/rbac/users/manage` (with `viewer` token)

```json
{
  "code": "FORBIDDEN",
  "message": "permission denied",
  "data": null
}
```

## Context Propagation

- `JwtAuthenticationFilter` parses JWT and writes `TenantContext` and `CurrentUserContext`
- context is cleared in `finally` for every request
- business code reads context through `ContextAccess`

## Expected RBAC Behavior

- `admin` can access `/api/v1/users`, `/api/v1/rbac/users`, `/api/v1/rbac/users/manage`, and `/api/v1/rbac/feature-flags`
- `ops` can access `/api/v1/users` and `/api/v1/rbac/users`
- `viewer` can access `/api/v1/users` and `/api/v1/rbac/users`
- `ops` and `viewer` are denied on endpoints requiring permissions they do not have

## Current User Management Boundary

- Swagger currently exposes only `GET /api/v1/users` under the `User Management` tag
- Week 2 DTO and service groundwork for detail, page, and write flows exists in code, but those routes are not callable until controller and contract methods are added
- Treat the Swagger-visible endpoints in this document as the only public API surface

## Tenant Isolation Note

Role and permission lookup during login is constrained by both `userId` and `tenantId` to reduce cross-tenant claim pollution risk when inconsistent link data exists.
