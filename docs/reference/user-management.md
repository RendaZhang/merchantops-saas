# User Management

Last updated: 2026-03-10

## Public API Surface

Swagger currently exposes one user-management endpoint:

| Method | Path | Auth | Permission | Notes |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/users` | Bearer JWT | `USER_READ` | Lists users for the current tenant only |

Use Swagger UI or [../../api-demo.http](../../api-demo.http) to call the current endpoint.

## `GET /api/v1/users`

Current behavior:

- tenant scope is derived from JWT and request context, not request parameters
- the controller resolves `tenantId` through `ContextAccess.requireTenantId()`
- the response is a JSON array, not a page object
- items are ordered by `id ASC`
- each item includes `id`, `username`, `displayName`, `email`, and `status`
- the seeded `demo-shop` tenant currently returns `admin`, `ops`, and `viewer`

Response example:

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

Verification references:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- [authentication-and-rbac.md](authentication-and-rbac.md): auth and permission expectations
- [api-docs.md](api-docs.md): Swagger/OpenAPI coverage notes
- [../runbooks/regression-checklist.md](../runbooks/regression-checklist.md): regression checks for `/api/v1/users`

## Non-Public Week 2 Groundwork

The current staged code adds internal groundwork for broader user management, but those parts are not yet public HTTP API:

- query-side DTOs and service methods exist for tenant-scoped user detail lookup, status filtering, and paged result shaping
- write-side command DTOs and `UserCommandService` exist for create and update flows
- a separate internal `updatePassword` method also exists, but password management is outside the current documented Week 2 public API target
- tenant-scoped username uniqueness checks are implemented in the repository/service layer
- write methods still stop at `UnsupportedOperationException`, so the flows are not implemented end to end yet

Until controller routes and OpenAPI contract methods are added, do not document those internal types as callable endpoints.
