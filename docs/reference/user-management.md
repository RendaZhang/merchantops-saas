# User Management

Last updated: 2026-03-10

## Public API Surface

Swagger currently exposes one user-management endpoint:

| Method | Path | Auth | Permission | Notes |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/users` | Bearer JWT | `USER_READ` | Pages users for the current tenant only |

Use Swagger UI or [../../api-demo.http](../../api-demo.http) to call the current endpoint.

## `GET /api/v1/users`

Current behavior:

- tenant scope is derived from JWT and request context, not request parameters
- the controller resolves `tenantId` through `ContextAccess.requireTenantId()`
- requires `USER_READ`, so `admin`, `ops`, and `viewer` can all query within their own tenant
- supports query parameters:
  - `page` with default `0`
  - `size` with default `10`
  - `username` as fuzzy match
  - `status` as exact match
  - `roleCode` as exact match
- the response is a page object with `items`, `page`, `size`, `total`, and `totalPages`
- items are ordered by `id ASC`
- each item includes `id`, `username`, `displayName`, `email`, and `status`

Example request:

```text
GET /api/v1/users?page=0&size=10&username=ad&status=ACTIVE&roleCode=TENANT_ADMIN
```

Response example:

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

Verification references:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- [authentication-and-rbac.md](authentication-and-rbac.md): auth and permission expectations
- [api-docs.md](api-docs.md): Swagger/OpenAPI coverage notes
- [../runbooks/regression-checklist.md](../runbooks/regression-checklist.md): regression checks for `/api/v1/users`
- query-side implementation lives in `UserQueryService` and always requires explicit `tenantId`
- write-side skeleton exists in `UserCommandService`, but create/update endpoints are still not public HTTP API
