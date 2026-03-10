# API Docs

## OpenAPI and Swagger URLs

- Base URL: `http://localhost:8080`
- OpenAPI JSON: `GET /v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Actuator OpenAPI group: available in Swagger (`springdoc.show-actuator=true`)

## Authentication in Swagger

- Security scheme: `bearerAuth` (HTTP Bearer JWT)
- Login endpoint: `POST /api/v1/auth/login`
- Demo tenant and users:
  - tenant: `demo-shop`
  - `admin / 123456`
  - `ops / 123456`
  - `viewer / 123456`

Swagger UI usability settings enabled in code:

- persist authorization between requests
- operations and tags sorted alphabetically
- try-it-out enabled by default
- request examples available on core write endpoints

## Annotation Organization

- OpenAPI annotations are centralized in `merchantops-api/src/main/java/com/renda/merchantops/api/contract`
- Controllers under `.../api/controller` keep business logic and permission checks only
- Reusable JSON examples are centralized in `merchantops-api/src/main/java/com/renda/merchantops/api/doc/OpenApiExamples.java`

## Endpoint Coverage Matrix

All documented business/health endpoints below are visible in Swagger UI.

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| `GET` | `/health` | No | Lightweight service health |
| `GET` | `/actuator/health` | No | Spring Boot actuator health |
| `POST` | `/api/v1/auth/login` | No | Login and get JWT token |
| `GET` | `/api/v1/dev/ping` | No | Dev ping test |
| `POST` | `/api/v1/dev/echo` | No | Dev echo test |
| `GET` | `/api/v1/dev/biz-error` | No | Dev error-shape test |
| `GET` | `/api/v1/user/me` | Yes | Current user profile |
| `GET` | `/api/v1/context` | Yes | Current tenant/user context |
| `GET` | `/api/v1/roles` | Yes + `USER_WRITE` | List assignable roles in current tenant |
| `GET` | `/api/v1/users` | Yes + `USER_READ` | Page users in current tenant |
| `POST` | `/api/v1/users` | Yes + `USER_WRITE` | Create an active user in current tenant |
| `PUT` | `/api/v1/users/{id}` | Yes + `USER_WRITE` | Update user profile fields |
| `PATCH` | `/api/v1/users/{id}/status` | Yes + `USER_WRITE` | Enable or disable a user |
| `PUT` | `/api/v1/users/{id}/roles` | Yes + `USER_WRITE` | Replace all tenant-local roles for a user |
| `GET` | `/api/v1/rbac/users` | Yes + `USER_READ` | RBAC demo read action |
| `GET` | `/api/v1/rbac/users/manage` | Yes + `USER_WRITE` | RBAC demo manage users |
| `GET` | `/api/v1/rbac/feature-flags` | Yes + `FEATURE_FLAG_MANAGE` | RBAC demo feature flags |

Notes about security whitelist routes:

- `/swagger-ui/**`, `/swagger-ui.html`, and `/v3/api-docs/**` are documentation resources rather than business API operations.
- They are intentionally not listed as OpenAPI operation paths.

User Management tag note:

- Swagger currently exposes `GET /api/v1/users`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, and `PUT /api/v1/users/{id}/roles` for user management.
- `GET /api/v1/users` supports `page`, `size`, `username`, `status`, and `roleCode` query parameters in Swagger.
- `POST /api/v1/users` exposes example payloads for username, password, and tenant-local role binding.
- `PUT /api/v1/users/{id}` exposes only `displayName` and `email`.
- `PATCH /api/v1/users/{id}/status` exposes only `ACTIVE` and `DISABLED`.
- `PUT /api/v1/users/{id}/roles` exposes `roleCodes` only and documents the forced re-login requirement after claim changes.
- Swagger exposes a separate `Role Management` tag for `GET /api/v1/roles`.
- User detail and write DTOs/services may exist in code, but they must not be treated as public API until contract/controller methods publish them into OpenAPI.
- See [user-management.md](user-management.md) for the current public contract and validation path.

## Core Endpoint Examples

### 1. Login (`POST /api/v1/auth/login`)

Request:

```json
{
  "tenantCode": "demo-shop",
  "username": "admin",
  "password": "123456"
}
```

Response:

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

### 2. Current User (`GET /api/v1/user/me`)

Response:

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

### 3. Tenant User List (`GET /api/v1/users`)

Response:

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

- this contract is a page object, not a bare array
- `page`, `size`, `username`, `status`, and `roleCode` query parameters are visible in Swagger
- the same request is available in [../../api-demo.http](../../api-demo.http)
- automated checks for the controller/query mapping live in [../runbooks/automated-tests.md](../runbooks/automated-tests.md)

### 4. Create User (`POST /api/v1/users`)

Request:

```json
{
  "username": "cashier",
  "displayName": "Cashier User",
  "email": "cashier@demo-shop.local",
  "password": "123456",
  "roleCodes": ["READ_ONLY"]
}
```

Response:

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

### 5. Update User (`PUT /api/v1/users/{id}`)

Request:

```json
{
  "displayName": "Updated Cashier",
  "email": "cashier.updated@demo-shop.local"
}
```

Response:

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

### 6. Disable User (`PATCH /api/v1/users/{id}/status`)

Request:

```json
{
  "status": "DISABLED"
}
```

Response:

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

### 7. Role List (`GET /api/v1/roles`)

Response:

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

### 8. Replace User Roles (`PUT /api/v1/users/{id}/roles`)

Request:

```json
{
  "roleCodes": ["TENANT_ADMIN"]
}
```

Response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "id": 3,
    "tenantId": 1,
    "username": "viewer",
    "roleCodes": ["TENANT_ADMIN"],
    "permissionCodes": ["USER_READ", "USER_WRITE", "ORDER_READ", "BILLING_READ", "FEATURE_FLAG_MANAGE"],
    "updatedAt": "2026-03-10T18:00:00"
  }
}
```

Current note:

- old JWT claims are rejected after this change; the user must login again to get a new token with the new roles and permissions

### 9. RBAC Denied Example (`GET /api/v1/rbac/users/manage` with viewer token)

Response:

```json
{
  "code": "FORBIDDEN",
  "message": "permission denied",
  "data": null
}
```

### 10. Health (`GET /health`)

Response:

```json
{
  "status": "UP",
  "service": "merchantops-saas"
}
```

## Stale Swagger Troubleshooting

- Confirm the process you are accessing is running on `http://localhost:8080`
- Refresh `/v3/api-docs` directly and then reload Swagger UI
