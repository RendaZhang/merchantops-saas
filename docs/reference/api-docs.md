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
| `GET` | `/api/v1/users` | Yes + `USER_READ` | List users in current tenant |
| `GET` | `/api/v1/rbac/users` | Yes + `USER_READ` | RBAC demo read action |
| `GET` | `/api/v1/rbac/users/manage` | Yes + `USER_WRITE` | RBAC demo manage users |
| `GET` | `/api/v1/rbac/feature-flags` | Yes + `FEATURE_FLAG_MANAGE` | RBAC demo feature flags |

Notes about security whitelist routes:

- `/swagger-ui/**`, `/swagger-ui.html`, and `/v3/api-docs/**` are documentation resources rather than business API operations.
- They are intentionally not listed as OpenAPI operation paths.

User Management tag note:

- Swagger currently exposes only `GET /api/v1/users` for user management.
- User detail, paging, and write DTOs/services may exist in code, but they must not be treated as public API until contract/controller methods publish them into OpenAPI.
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

- today this contract is list-only and returns an array, not a page object
- `page`, `size`, and `status` query parameters are not visible in Swagger yet
- the same request is available in [../../api-demo.http](../../api-demo.http)

### 4. RBAC Denied Example (`GET /api/v1/rbac/users/manage` with viewer token)

Response:

```json
{
  "code": "FORBIDDEN",
  "message": "permission denied",
  "data": null
}
```

### 5. Health (`GET /health`)

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
