# User Management

Last updated: 2026-03-28

## Public API Surface

Swagger currently exposes seven user-management endpoints plus one companion role-query endpoint:

| Method | Path | Auth | Permission | Notes |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/users` | Bearer JWT | `USER_READ` | Pages users for the current tenant only |
| `GET` | `/api/v1/users/{id}` | Bearer JWT | `USER_READ` | Returns one current-tenant user with current role codes |
| `POST` | `/api/v1/users` | Bearer JWT | `USER_WRITE` | Creates an `ACTIVE` user in the current tenant and binds tenant-local roles |
| `PUT` | `/api/v1/users/{id}` | Bearer JWT | `USER_WRITE` | Updates mutable profile fields only |
| `PATCH` | `/api/v1/users/{id}/status` | Bearer JWT | `USER_WRITE` | Enables or disables a user in the current tenant |
| `PUT` | `/api/v1/users/{id}/roles` | Bearer JWT | `USER_WRITE` | Replaces all roles for a tenant user |
| `POST` | `/api/v1/users/{id}/disable-requests` | Bearer JWT | `USER_WRITE` | Creates a minimal approval request for disabling a user (status stays unchanged until approved) |

Companion role endpoint:

- `GET /api/v1/roles` with `USER_WRITE` lists the current tenant roles that can be assigned through the role-replacement endpoint

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
- [../contributing/development-agent-guidance.md](../contributing/development-agent-guidance.md): repository, service, DTO, documentation, and testing extension rules
- [../runbooks/automated-tests.md](../runbooks/automated-tests.md): current automated coverage and recommended Maven commands
- [../runbooks/local-smoke-test.md](../runbooks/local-smoke-test.md): live verification flow and smoke-data cleanup
- [../runbooks/regression-checklist.md](../runbooks/regression-checklist.md): regression checks for `/api/v1/users`

## `GET /api/v1/users/{id}`

Current behavior:

- tenant scope is derived from JWT and request context, not request parameters
- requires `USER_READ`, so seeded `admin`, `ops`, and `viewer` can all view current-tenant detail
- returns one user from the current tenant only
- response includes `id`, `tenantId`, `username`, `displayName`, `email`, `status`, `roleCodes`, `createdAt`, and `updatedAt`
- returns `404` if the target user is not in the current tenant

Example request:

```text
GET /api/v1/users/3
```

Response example:

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

## `POST /api/v1/users`

Current behavior:

- tenant scope is derived from JWT and request context, not request parameters
- requires `USER_WRITE`, so seeded `admin` can create users but `ops` and `viewer` cannot
- request fields:
  - `username`
  - `displayName`
  - `email`
  - `password`
  - `roleCodes`
- same-tenant username must be unique
- password is stored as BCrypt
- password must not start or end with whitespace
- new users are always created with status `ACTIVE`
- every requested role code must already exist in the current tenant
- cross-tenant role binding is rejected

Example request:

```json
{
  "username": "cashier",
  "displayName": "Cashier User",
  "email": "cashier@demo-shop.local",
  "password": "123456",
  "roleCodes": ["READ_ONLY"]
}
```

Response example:

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

Failure examples:

- duplicate username: `BAD_REQUEST`, message `username already exists in tenant`
- invalid or cross-tenant role code: `BAD_REQUEST`, message `roleCodes must exist in current tenant`
- password with leading or trailing whitespace: `VALIDATION_ERROR`, message `password: password must not start or end with whitespace`

Implementation notes:

- query-side implementation lives in `UserQueryService` and always requires explicit `tenantId`
- write-side implementation lives in `UserCommandService` and wraps create work in a transaction
- `user_role` writes are derived from tenant-filtered `role` lookups rather than trusting request role IDs
- password handling is regression-sensitive: create and login must enforce the same rule, especially for leading and trailing whitespace

## `PUT /api/v1/users/{id}`

Current behavior:

- tenant scope is derived from JWT and request context, not request parameters
- requires `USER_WRITE`
- only `displayName` and `email` are mutable here
- `tenantId`, `username`, and `passwordHash` are not accepted from the public request
- `displayName` is required
- blank `email` clears the stored value
- returns `404` if the target user is not in the current tenant

Example request:

```json
{
  "displayName": "Updated Cashier",
  "email": "cashier.updated@demo-shop.local"
}
```

Response example:

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

## `PATCH /api/v1/users/{id}/status`

Current behavior:

- tenant scope is derived from JWT and request context, not request parameters
- requires `USER_WRITE`
- request body accepts only `ACTIVE` or `DISABLED`
- status management is split out from the general profile update flow on purpose
- disabling a user prevents future logins because login requires `ACTIVE`
- tokens issued before the disable operation are also rejected on protected endpoints because request authentication re-checks the current user status
- returns `404` if the target user is not in the current tenant

Example request:

```json
{
  "status": "DISABLED"
}
```


## `POST /api/v1/users/{id}/disable-requests`

Current behavior:

- tenant scope is derived from JWT and request context
- requires `USER_WRITE`
- creates `approval_request` with action `USER_STATUS_DISABLE` and initial status `PENDING`
- target user status is not changed when request is created
- rejects duplicate pending disable requests for the same user
- requester cannot later self-approve or self-reject this same request

Related approval endpoints:

- `GET /api/v1/approval-requests` (`USER_READ`)
- `GET /api/v1/approval-requests/{id}` (`USER_READ`)
- `POST /api/v1/approval-requests/{id}/approve` (`USER_WRITE`)
- `POST /api/v1/approval-requests/{id}/reject` (`USER_WRITE`)

Example response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "id": 901,
    "tenantId": 1,
    "actionType": "USER_STATUS_DISABLE",
    "entityType": "USER",
    "entityId": 5,
    "requestedBy": 5,
    "reviewedBy": null,
    "status": "PENDING",
    "payloadJson": "{\"status\":\"DISABLED\"}",
    "requestId": "disable-req-create-1",
    "createdAt": "2026-03-12T10:10:00",
    "reviewedAt": null,
    "executedAt": null
  }
}
```

Approve path reuses existing `PATCH /api/v1/users/{id}/status` write chain internally to set `DISABLED`, so tenant/operator/requestId/audit governance remains aligned with existing user status writes.

## `PUT /api/v1/users/{id}/roles`

Current behavior:

- tenant scope is derived from JWT and request context, not request parameters
- requires `USER_WRITE`
- request body accepts `roleCodes` only
- every role code must already exist in the current tenant
- role reassignment clears previous `user_role` rows first, then writes the new set
- role and permission claim changes make old JWT tokens stale immediately
- the affected user must login again to get a new token with the new roles and permissions
- returns `404` if the target user is not in the current tenant

Example request:

```json
{
  "roleCodes": ["TENANT_ADMIN"]
}
```

Response example:

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

## `GET /api/v1/roles`

Current behavior:

- tenant scope is derived from JWT and request context, not request parameters
- requires `USER_WRITE`
- returns the current tenant roles that are valid for `PUT /api/v1/users/{id}/roles`
- response is a lightweight `items` collection rather than a page because tenant role count is expected to stay small

Response example:

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

## Developer Guardrails

For future user-management work:

- keep `tenantId` resolution at the controller edge, then pass it explicitly downward
- keep query DTOs under `dto/user/query`
- keep role-query DTOs under `dto/role/query`
- keep write DTOs under `dto/user/command`
- keep `/api/v1/users` as the baseline example for tenant-aware paging
- keep profile mutation and status mutation as separate commands and endpoints
- keep role reassignment isolated behind `PUT /api/v1/users/{id}/roles`; do not overload profile update or create flows
- if password validation changes, add create + login regression coverage for whitespace edge cases before updating public docs
- do not publish new user-management endpoints without updating Swagger, docs, `api-demo.http`, and runbooks together
- use [../contributing/development-agent-guidance.md](../contributing/development-agent-guidance.md) as the implementation baseline

## Automated Coverage Focus

Current automated tests for user management focus on:

- real login -> JWT -> `/api/v1/users` integration coverage for `GET`, `POST`, `PUT`, `PATCH`, and `PUT /api/v1/users/{id}/roles`
- real login -> JWT -> `/api/v1/roles` integration coverage for role lookup
- real login -> JWT -> `POST /api/v1/users/{id}/disable-requests` coverage for the approval-backed disable-request entry path
- tenant and permission claims generated by the production JWT service
- HTTP request binding and controller forwarding of tenant context
- unauthenticated / unauthorized / success response paths for `/api/v1/users` and `/api/v1/roles`
- create-user validation, permission, cross-tenant role rejection, and BCrypt-backed login after creation
- update-user validation and profile persistence
- disable-user status flow, login rejection, and old-token rejection for `DISABLED` users
- duplicate pending disable-request rejection plus approval queue normalization and stable ordering in the approval service layer
- role reassignment flow, stale-token rejection after claim changes, and re-login with new permissions
- query-side page normalization and DTO mapping
- query-side list and username-exists helper paths
- detail lookup success mapping, role-code hydration, and `NOT_FOUND` handling
- command-side duplicate-username, role-scope rejection, profile update, status update, and role reassignment behavior
- repository-level verification of the native paged search query for tenant, username, status, roleCode, and deduplication behavior

Current automated tests do not replace:

- authenticated verification for endpoints outside the covered login + `/api/v1/roles` + `/api/v1/users` + `/api/v1/tickets` flow
- Swagger rendering validation

Use [../runbooks/local-smoke-test.md](../runbooks/local-smoke-test.md) after the automated suite passes.
