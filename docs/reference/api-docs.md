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
| `GET` | `/api/v1/users/{id}` | Yes + `USER_READ` | Get one tenant-scoped user detail |
| `POST` | `/api/v1/users` | Yes + `USER_WRITE` | Create an active user in current tenant |
| `PUT` | `/api/v1/users/{id}` | Yes + `USER_WRITE` | Update user profile fields |
| `PATCH` | `/api/v1/users/{id}/status` | Yes + `USER_WRITE` | Enable or disable a user |
| `PUT` | `/api/v1/users/{id}/roles` | Yes + `USER_WRITE` | Replace all tenant-local roles for a user |
| `POST` | `/api/v1/users/{id}/disable-requests` | Yes + `USER_WRITE` | Create a pending approval request for disabling a user |
| `GET` | `/api/v1/tickets` | Yes + `TICKET_READ` | Page tickets in current tenant |
| `GET` | `/api/v1/tickets/{id}` | Yes + `TICKET_READ` | Get one tenant-scoped ticket detail with comments and workflow logs |
| `GET` | `/api/v1/tickets/{id}/ai-interactions` | Yes + `TICKET_READ` | Page narrowed stored AI interaction history for one current-tenant ticket |
| `POST` | `/api/v1/tickets/{id}/ai-summary` | Yes + `TICKET_READ` | Generate a suggestion-only AI summary for one current-tenant ticket |
| `POST` | `/api/v1/tickets/{id}/ai-triage` | Yes + `TICKET_READ` | Generate a suggestion-only AI triage result for one current-tenant ticket |
| `POST` | `/api/v1/tickets/{id}/ai-reply-draft` | Yes + `TICKET_READ` | Generate a suggestion-only AI internal reply draft for one current-tenant ticket |
| `POST` | `/api/v1/tickets` | Yes + `TICKET_WRITE` | Create a new `OPEN` ticket |
| `PATCH` | `/api/v1/tickets/{id}/assignee` | Yes + `TICKET_WRITE` | Assign the ticket to an active user in current tenant |
| `PATCH` | `/api/v1/tickets/{id}/status` | Yes + `TICKET_WRITE` | Transition the ticket status |
| `POST` | `/api/v1/tickets/{id}/comments` | Yes + `TICKET_WRITE` | Add a comment and workflow log entry |
| `GET` | `/api/v1/audit-events` | Yes + `USER_READ` | List current-tenant audit rows for one entity |
| `GET` | `/api/v1/approval-requests` | Yes + `USER_READ` | Page approval requests in current tenant |
| `GET` | `/api/v1/approval-requests/{id}` | Yes + `USER_READ` | Get one tenant-scoped approval request |
| `POST` | `/api/v1/approval-requests/{id}/approve` | Yes + `USER_WRITE` | Approve a pending request and execute the action |
| `POST` | `/api/v1/approval-requests/{id}/reject` | Yes + `USER_WRITE` | Reject a pending request |
| `POST` | `/api/v1/import-jobs` | Yes + `USER_WRITE` | Create an async import job from multipart request + CSV file |
| `GET` | `/api/v1/import-jobs` | Yes + `USER_READ` | Page import jobs in current tenant with optional queue filters |
| `GET` | `/api/v1/import-jobs/{id}` | Yes + `USER_READ` | Get one tenant-scoped import job overview with `errorCodeCounts` plus `itemErrors` |
| `POST` | `/api/v1/import-jobs/{id}/replay-failures` | Yes + `USER_WRITE` | Create a new derived import job from the source job's replayable failed rows |
| `POST` | `/api/v1/import-jobs/{id}/replay-file` | Yes + `USER_WRITE` | Create a new derived import job by copying the stored source file from a full-failure source job |
| `POST` | `/api/v1/import-jobs/{id}/replay-failures/selective` | Yes + `USER_WRITE` | Create a new derived import job from the source job's replayable failed rows whose `errorCode` exactly matches one of the requested values |
| `POST` | `/api/v1/import-jobs/{id}/replay-failures/selective/proposals` | Yes + `USER_WRITE` | Create a pending approval request for a later selective replay execution of the source job |
| `POST` | `/api/v1/import-jobs/{id}/replay-failures/edited` | Yes + `USER_WRITE` | Create a new derived import job from caller-provided full replacement rows keyed by replayable failed-row `errorId` |
| `GET` | `/api/v1/import-jobs/{id}/errors` | Yes + `USER_READ` | Page one tenant-scoped import job's failure items with optional `errorCode` filter |
| `GET` | `/api/v1/import-jobs/{id}/ai-interactions` | Yes + `USER_READ` | Page narrowed stored AI interaction history for one current-tenant import job |
| `POST` | `/api/v1/import-jobs/{id}/ai-error-summary` | Yes + `USER_READ` | Generate a suggestion-only AI error summary for one current-tenant import job |
| `POST` | `/api/v1/import-jobs/{id}/ai-mapping-suggestion` | Yes + `USER_READ` | Generate a suggestion-only AI mapping proposal for one current-tenant import job |
| `POST` | `/api/v1/import-jobs/{id}/ai-fix-recommendation` | Yes + `USER_READ` | Generate a suggestion-only AI fix recommendation for one current-tenant import job |
| `GET` | `/api/v1/rbac/users` | Yes + `USER_READ` | RBAC demo read action |
| `GET` | `/api/v1/rbac/users/manage` | Yes + `USER_WRITE` | RBAC demo manage users |
| `GET` | `/api/v1/rbac/feature-flags` | Yes + `FEATURE_FLAG_MANAGE` | RBAC demo feature flags |

Notes about security whitelist routes:

- `/swagger-ui/**`, `/swagger-ui.html`, and `/v3/api-docs/**` are documentation resources rather than business API operations.
- They are intentionally not listed as OpenAPI operation paths.

User Management tag note:

- Swagger currently exposes `GET /api/v1/users`, `GET /api/v1/users/{id}`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/status`, `PUT /api/v1/users/{id}/roles`, and `POST /api/v1/users/{id}/disable-requests` for user management.
- `GET /api/v1/users` supports `page`, `size`, `username`, `status`, and `roleCode` query parameters in Swagger.
- `GET /api/v1/users/{id}` returns one tenant-scoped user plus current `roleCodes`.
- `POST /api/v1/users` exposes example payloads for username, password, and tenant-local role binding.
- `PUT /api/v1/users/{id}` exposes only `displayName` and `email`.
- `PATCH /api/v1/users/{id}/status` exposes only `ACTIVE` and `DISABLED`.
- `PUT /api/v1/users/{id}/roles` exposes `roleCodes` only and documents the forced re-login requirement after claim changes.
- `POST /api/v1/users/{id}/disable-requests` creates a `PENDING` approval request and leaves user status unchanged until approval.
- Swagger exposes a separate `Role Management` tag for `GET /api/v1/roles`.
- See [user-management.md](user-management.md) for the current public contract and validation path.

Ticket Workflow tag note:

- Swagger currently exposes `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}`, `GET /api/v1/tickets/{id}/ai-interactions`, `POST /api/v1/tickets/{id}/ai-summary`, `POST /api/v1/tickets/{id}/ai-triage`, `POST /api/v1/tickets/{id}/ai-reply-draft`, `POST /api/v1/tickets`, `PATCH /api/v1/tickets/{id}/assignee`, `PATCH /api/v1/tickets/{id}/status`, and `POST /api/v1/tickets/{id}/comments`.
- `GET /api/v1/tickets` supports `page`, `size`, `status`, `assigneeId`, `keyword` (title/description), and `unassignedOnly`.
- `GET /api/v1/tickets/{id}` includes current comments and workflow-level operation logs.
- `GET /api/v1/tickets/{id}/ai-interactions` supports `page`, `size`, `interactionType`, and `status`, with stable ordering `createdAt DESC, id DESC`.
- `GET /api/v1/tickets/{id}/ai-interactions` exposes narrowed runtime metadata including `usagePromptTokens`, `usageCompletionTokens`, `usageTotalTokens`, and `usageCostMicros`, while still not exposing raw prompt text or raw provider payload and while remaining outside billing or ledger semantics.
- `POST /api/v1/tickets/{id}/ai-reply-draft` exposes no request body and returns a structured internal comment draft plus assembled `draftText`.
- `POST /api/v1/tickets` always creates an `OPEN` ticket.
- `PATCH /api/v1/tickets/{id}/assignee` only accepts an active assignee from the current tenant.
- `PATCH /api/v1/tickets/{id}/status` documents the current transition rules for `OPEN`, `IN_PROGRESS`, and `CLOSED`, including reopen (`CLOSED -> OPEN`).
- `POST /api/v1/tickets/{id}/comments` exposes comment content only; operator and request tracing are derived server-side.
- See [ticket-workflow.md](ticket-workflow.md) for the current public contract and workflow notes.

Audit Events tag note:

- Swagger currently exposes `GET /api/v1/audit-events`.
- the endpoint requires `USER_READ`.
- query params are `entityType` and `entityId`.
- `entityType` is case-insensitive in the current implementation and is normalized internally before lookup.
- the current read shape is minimal: entity-scoped lookup only, ordered by insert id ascending, with no pagination or approval queue semantics yet.
- current public write flows emit audit rows for `USER`, `TICKET`, and `APPROVAL_REQUEST` entities.
- See [audit-approval.md](audit-approval.md) for the current governance baseline and non-goals.

Approval Requests tag note:

- Swagger currently exposes `GET /api/v1/approval-requests`, `GET /api/v1/approval-requests/{id}`, `POST /api/v1/approval-requests/{id}/approve`, and `POST /api/v1/approval-requests/{id}/reject`.
- `GET /api/v1/approval-requests` requires `USER_READ` and supports `page`, `size`, `status`, `actionType`, and `requestedBy`.
- `GET /api/v1/approval-requests/{id}` requires `USER_READ`.
- approve/reject endpoints require `USER_WRITE`.
- the current public approval surface supports two action types: `USER_STATUS_DISABLE` and `IMPORT_JOB_SELECTIVE_REPLAY`.
- requester cannot approve or reject the same request they created.
- approval is synchronous in the current implementation and reuses the existing user status write flow for `USER_STATUS_DISABLE` plus the existing import selective replay flow for `IMPORT_JOB_SELECTIVE_REPLAY`.

Import Jobs tag note:

- Swagger currently exposes `POST /api/v1/import-jobs`, `GET /api/v1/import-jobs`, `GET /api/v1/import-jobs/{id}`, `GET /api/v1/import-jobs/{id}/ai-interactions`, `POST /api/v1/import-jobs/{id}/ai-error-summary`, `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`, `POST /api/v1/import-jobs/{id}/ai-fix-recommendation`, `POST /api/v1/import-jobs/{id}/replay-failures`, `POST /api/v1/import-jobs/{id}/replay-file`, `POST /api/v1/import-jobs/{id}/replay-failures/selective`, `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals`, `POST /api/v1/import-jobs/{id}/replay-failures/edited`, and `GET /api/v1/import-jobs/{id}/errors`.
- `POST /api/v1/import-jobs`, `POST /api/v1/import-jobs/{id}/replay-failures`, `POST /api/v1/import-jobs/{id}/replay-file`, `POST /api/v1/import-jobs/{id}/replay-failures/selective`, `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals`, and `POST /api/v1/import-jobs/{id}/replay-failures/edited` require `USER_WRITE`.
- `GET /api/v1/import-jobs`, `GET /api/v1/import-jobs/{id}`, `GET /api/v1/import-jobs/{id}/errors`, `GET /api/v1/import-jobs/{id}/ai-interactions`, `POST /api/v1/import-jobs/{id}/ai-error-summary`, `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`, and `POST /api/v1/import-jobs/{id}/ai-fix-recommendation` require `USER_READ`.
- `GET /api/v1/import-jobs` now exposes `page`, `size`, `status`, `importType`, `requestedBy`, and `hasFailuresOnly`.
- `GET /api/v1/import-jobs/{id}` detail now exposes nullable `sourceJobId` for replay-derived jobs.
- `GET /api/v1/import-jobs/{id}/ai-interactions` supports `page`, `size`, `interactionType`, and `status`, with stable ordering `createdAt DESC, id DESC`.
- `GET /api/v1/import-jobs/{id}/ai-interactions` exposes narrowed runtime metadata including `usagePromptTokens`, `usageCompletionTokens`, `usageTotalTokens`, and `usageCostMicros`, while still not exposing raw prompt text or raw provider payload and while remaining outside billing or ledger semantics.
- `POST /api/v1/import-jobs/{id}/ai-error-summary` exposes no request body and returns suggestion-only fields `importJobId`, `summary`, `topErrorPatterns`, `recommendedNextSteps`, `promptVersion`, `modelId`, `generatedAt`, `latencyMs`, and `requestId`.
- `POST /api/v1/import-jobs/{id}/ai-error-summary` derives prompt context from import detail, `errorCodeCounts`, and the first 20 failure rows after local sanitization; it does not forward raw `itemErrors.rawPayload` values.
- `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion` exposes no request body and returns suggestion-only fields `importJobId`, `summary`, `suggestedFieldMappings`, `confidenceNotes`, `recommendedOperatorChecks`, `promptVersion`, `modelId`, `generatedAt`, `latencyMs`, and `requestId`.
- `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion` is currently limited to `USER_CSV` jobs that already have failure signal plus parseable sanitized header/global signal; it does not rescan the source file or forward raw row values.
- `POST /api/v1/import-jobs/{id}/ai-fix-recommendation` exposes no request body and returns suggestion-only fields `importJobId`, `summary`, `recommendedFixes`, `confidenceNotes`, `recommendedOperatorChecks`, `promptVersion`, `modelId`, `generatedAt`, `latencyMs`, and `requestId`.
- `POST /api/v1/import-jobs/{id}/ai-fix-recommendation` is currently limited to `USER_CSV` jobs that already have failure signal plus grounded sanitized row-level error groups; it does not rescan the source file, return direct replacement values, or forward raw row values.
- `POST /api/v1/import-jobs/{id}/replay-failures` creates a new derived `QUEUED` job from replayable failed rows only; it does not reset the old job.
- `POST /api/v1/import-jobs/{id}/replay-file` copies the stored source file into a new derived `QUEUED` job for current-tenant `FAILED` `USER_CSV` source jobs that have no successful rows, and records `replayMode=WHOLE_FILE` in source/replay audit snapshots.
- `POST /api/v1/import-jobs/{id}/replay-failures/selective` creates a new derived `QUEUED` job from replayable failed rows whose `errorCode` exactly matches one of the requested values.
- `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals` returns the shared approval-request response shape and stores only safe proposal payload fields (`sourceJobId`, normalized `errorCodes`, optional `sourceInteractionId`, optional `proposalReason`) for later human review.
- `POST /api/v1/import-jobs/{id}/replay-failures/edited` creates a new derived `QUEUED` job from caller-provided full replacement rows keyed by replayable failed-row `errorId`.
- `GET /api/v1/import-jobs/{id}/errors` now exposes `page`, `size`, and `errorCode`.
- list ordering is currently `createdAt DESC, id DESC`.
- detail returns `errorCodeCounts` for quick triage plus backward-compatible row-level `itemErrors`.
- `/errors` pages the same failure rows with stable ordering: null `rowNumber` first, then `rowNumber ASC, id ASC`.
- row-level `itemErrors` now include both parse failures and business-row execution failures for `USER_CSV`.
- See [import-jobs.md](import-jobs.md) for the current async-import contract and non-goals.

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
    "permissions": ["USER_READ", "USER_WRITE", "ORDER_READ", "BILLING_READ", "FEATURE_FLAG_MANAGE", "TICKET_READ", "TICKET_WRITE"]
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

### 4. Tenant User Detail (`GET /api/v1/users/{id}`)

Response:

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
- returns `404` when the target user is outside the current tenant
- useful as the current read-side detail endpoint before any admin UI exists

### 5. Create User (`POST /api/v1/users`)

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

### 6. Update User (`PUT /api/v1/users/{id}`)

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

### 7. Disable User (`PATCH /api/v1/users/{id}/status`)

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

### 8. Role List (`GET /api/v1/roles`)

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

### 9. Replace User Roles (`PUT /api/v1/users/{id}/roles`)

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
    "permissionCodes": ["USER_READ", "USER_WRITE", "ORDER_READ", "BILLING_READ", "FEATURE_FLAG_MANAGE", "TICKET_READ", "TICKET_WRITE"],
    "updatedAt": "2026-03-10T18:00:00"
  }
}
```

Current note:

- old JWT claims are rejected after this change; the user must login again to get a new token with the new roles and permissions

### 10. RBAC Denied Example (`GET /api/v1/rbac/users/manage` with viewer token)

Response:

```json
{
  "code": "FORBIDDEN",
  "message": "permission denied",
  "data": null
}
```

### 11. Health (`GET /health`)

Response:

```json
{
  "status": "UP",
  "service": "merchantops-saas"
}
```

### 12. Ticket List (`GET /api/v1/tickets`)

Response:

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
- list uses a page object, not a bare array
- current filter set is `page`, `size`, `status`, `assigneeId`, `keyword`, and `unassignedOnly`
- see [ticket-workflow.md](ticket-workflow.md) for the closeable-loop behavior

### 13. Create Ticket (`POST /api/v1/tickets`)

Request:

```json
{
  "title": "POS register frozen",
  "description": "Register screen froze during checkout."
}
```

Response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "id": 402,
    "tenantId": 1,
    "title": "POS register frozen",
    "description": "Register screen froze during checkout.",
    "status": "OPEN",
    "assigneeId": null,
    "assigneeUsername": null,
    "createdAt": "2026-03-11T11:00:00",
    "updatedAt": "2026-03-11T11:00:00"
  }
}
```

### 14. Ticket Detail (`GET /api/v1/tickets/{id}`)

Response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "id": 402,
    "tenantId": 1,
    "title": "POS register frozen",
    "description": "Register screen froze during checkout.",
    "status": "CLOSED",
    "assigneeId": 2,
    "assigneeUsername": "ops",
    "createdBy": 1,
    "createdByUsername": "admin",
    "createdAt": "2026-03-11T11:00:00",
    "updatedAt": "2026-03-11T11:15:00",
    "comments": [
      {
        "id": 51,
        "ticketId": 402,
        "content": "Investigating store terminal logs.",
        "createdBy": 2,
        "createdByUsername": "ops",
        "createdAt": "2026-03-11T11:10:00"
      }
    ],
    "operationLogs": [
      {
        "id": 61,
        "operationType": "CREATED",
        "detail": "ticket created",
        "operatorId": 1,
        "operatorUsername": "admin",
        "createdAt": "2026-03-11T11:00:00"
      }
    ]
  }
}
```

### 15. Audit Event Query (`GET /api/v1/audit-events`)

Response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "items": [
      {
        "id": 701,
        "entityType": "TICKET",
        "entityId": 402,
        "actionType": "TICKET_STATUS_UPDATED",
        "operatorId": 1,
        "requestId": "req-demo-402-status",
        "beforeValue": "{\"status\":\"IN_PROGRESS\"}",
        "afterValue": "{\"status\":\"CLOSED\"}",
        "approvalStatus": "NOT_REQUIRED",
        "createdAt": "2026-03-11T11:15:00"
      }
    ]
  }
}
```

Current notes:

- requires `USER_READ`
- current query is entity-scoped only: `entityType + entityId`
- current public entity types are `USER`, `TICKET`, and `APPROVAL_REQUEST`
- `approvalStatus` is currently always `NOT_REQUIRED` in Week 4 Slice A

### 16. Create User Disable Request (`POST /api/v1/users/{id}/disable-requests`)

Response:

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

Current notes:

- requires `USER_WRITE`
- creates a request only; it does not disable the target user immediately
- duplicate pending requests for the same tenant user are rejected

### 17. Approval Request Review (`GET /api/v1/approval-requests/{id}` / `POST /approve`)

Response after approval:

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
    "reviewedBy": 1,
    "status": "APPROVED",
    "payloadJson": "{\"status\":\"DISABLED\"}",
    "requestId": "disable-req-create-1",
    "createdAt": "2026-03-12T10:10:00",
    "reviewedAt": "2026-03-12T10:12:00",
    "executedAt": "2026-03-12T10:12:00"
  }
}
```

Current notes:

- `GET /api/v1/approval-requests/{id}` requires `USER_READ`
- approve/reject requires `USER_WRITE`
- requester cannot self-approve or self-reject
- approve path executes the disable action immediately in the same transaction boundary

### 18. Approval Request Queue (`GET /api/v1/approval-requests`)

Response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "items": [
      {
        "id": 901,
        "actionType": "USER_STATUS_DISABLE",
        "entityType": "USER",
        "entityId": 5,
        "requestedBy": 5,
        "reviewedBy": null,
        "status": "PENDING",
        "createdAt": "2026-03-12T10:10:00",
        "reviewedAt": null,
        "executedAt": null
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

- requires `USER_READ`
- queue supports `page`, `size`, `status`, `actionType`, and `requestedBy`
- current ordering is stable: `createdAt DESC, id DESC`
- the same request variants are available in [../../api-demo.http](../../api-demo.http)

### 19. Import Job List (`GET /api/v1/import-jobs`)

Response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "items": [
      {
        "id": 1201,
        "importType": "USER_CSV",
        "sourceType": "CSV",
        "sourceFilename": "users.csv",
        "status": "SUCCEEDED",
        "requestedBy": 1,
        "hasFailures": false,
        "totalCount": 1,
        "successCount": 1,
        "failureCount": 0,
        "errorSummary": null,
        "createdAt": "2026-03-12T16:20:00",
        "startedAt": "2026-03-12T16:20:02",
        "finishedAt": "2026-03-12T16:20:03"
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

- requires `USER_READ`
- the current query shape exposes `page`, `size`, `status`, `importType`, `requestedBy`, and `hasFailuresOnly`
- current list ordering is `createdAt DESC, id DESC`
- `hasFailuresOnly=true` returns both partial-success jobs (`SUCCEEDED` with `failureCount > 0`) and terminal `FAILED` jobs
- detail response also exposes `errorCodeCounts` plus `itemErrors` for both parse/header failures and business-row execution failures in the current `USER_CSV` path

### 20. Import Job Replay (`POST /api/v1/import-jobs/{id}/replay-failures`)

Response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "id": 1202,
    "importType": "USER_CSV",
    "sourceFilename": "replay-failures-job-1201.csv",
    "sourceJobId": 1201,
    "status": "QUEUED"
  }
}
```

Current notes:

- requires `USER_WRITE`
- source job must be current-tenant, terminal, `USER_CSV`, and contain replayable failed rows
- replay creates a new derived job instead of mutating the source job in place
- the new job's detail now exposes `sourceJobId`

### 21. Whole-File Import Job Replay (`POST /api/v1/import-jobs/{id}/replay-file`)

Response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "id": 1203,
    "importType": "USER_CSV",
    "sourceFilename": "replay-file-job-1201.csv",
    "sourceJobId": 1201,
    "status": "QUEUED"
  }
}
```

Current notes:

- requires `USER_WRITE`
- source job must be current-tenant, `FAILED`, `USER_CSV`, and have no successful rows
- whole-file replay creates a new derived job instead of mutating the source job in place
- whole-file replay copies the stored source file bytes through the current storage abstraction
- source and replay audit snapshots add `replayMode=WHOLE_FILE`

### 22. Selective Import Job Replay (`POST /api/v1/import-jobs/{id}/replay-failures/selective`)

Request:

```json
{
  "errorCodes": ["UNKNOWN_ROLE"]
}
```

Current notes:

- requires `USER_WRITE`
- matching is by exact `errorCode` value from the source job's replayable row failures
- empty `errorCodes`, cross-tenant source jobs, and selections with no replayable matching rows are rejected
- source and replay audit snapshots record `selectedErrorCodes` in this slice instead of adding a new import-job column
- response shape stays the same as the standard replay response above because the selective criteria only live in audit snapshots for this slice

### 23. Edited Import Job Replay (`POST /api/v1/import-jobs/{id}/replay-failures/edited`)

Request:

```json
{
  "items": [
    {
      "errorId": 32,
      "username": "beta",
      "displayName": "Beta User",
      "email": "beta@example.com",
      "password": "abc123",
      "roleCodes": ["READ_ONLY"]
    }
  ]
}
```

Current notes:

- requires `USER_WRITE`
- matching is by exact replayable failed-row `errorId` from detail `itemErrors` or `/errors`
- each item is a full replacement row, not a sparse patch
- duplicate `errorId`, cross-job / cross-tenant `errorId`, and header/global error targets are rejected
- source and replay audit snapshots record `editedErrorIds`, `editedRowCount`, and `editedFields` only; replacement values are intentionally excluded

### 24. Import Job Errors (`GET /api/v1/import-jobs/{id}/errors`)

Response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "items": [
      {
        "id": 31,
        "rowNumber": 3,
        "errorCode": "DUPLICATE_USERNAME",
        "errorMessage": "username already exists in current tenant",
        "rawPayload": "admin,Duplicate User,dup@example.com,abc123,READ_ONLY",
        "createdAt": "2026-03-12T18:20:04"
      }
    ],
    "page": 0,
    "size": 1,
    "total": 2,
    "totalPages": 2
  }
}
```

Current notes:

- requires `USER_READ`
- the current query shape exposes `page`, `size`, and exact `errorCode`
- failure rows are ordered stably: null `rowNumber` first, then `rowNumber ASC, id ASC`
- this is the preferred read surface for larger jobs; detail remains the overview surface

### 25. Import AI Error Summary (`POST /api/v1/import-jobs/{id}/ai-error-summary`)

Response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "importJobId": 1201,
    "summary": "The job is primarily blocked by tenant role validation failures, with a smaller duplicate-username tail. The sampled failed rows are structurally complete, so role-map cleanup should come before any replay attempt.",
    "topErrorPatterns": [
      "UNKNOWN_ROLE is the dominant error code in both the aggregate counts and the sampled failed rows.",
      "Most sampled failed rows still contain all expected `USER_CSV` columns, so the failures look data-quality related rather than parser-shape related."
    ],
    "recommendedNextSteps": [
      "Confirm the valid tenant role codes that should replace the invalid mappings before replay.",
      "Review duplicate usernames separately because those rows need edits rather than role-map cleanup."
    ],
    "promptVersion": "import-error-summary-v1",
    "modelId": "gpt-4.1-mini",
    "generatedAt": "2026-03-27T10:25:15",
    "latencyMs": 512,
    "requestId": "import-ai-error-summary-req-1"
  }
}
```

Current notes:

- requires `USER_READ`
- request body is empty
- cross-tenant or missing jobs return `404`
- the endpoint is read-only and suggestion-only; it does not mutate import jobs, item errors, or replay state
- raw `itemErrors.rawPayload` stays out of the AI prompt; the service only sends row metadata plus a structural-only summary

### 26. Import AI Mapping Suggestion (`POST /api/v1/import-jobs/{id}/ai-mapping-suggestion`)

Response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "importJobId": 1202,
    "summary": "The failed header still looks close to the canonical USER_CSV schema, so the safest next step is to confirm the proposed field mappings before preparing any replay input.",
    "suggestedFieldMappings": [
      {
        "canonicalField": "username",
        "observedColumnSignal": {
          "headerName": "login",
          "headerPosition": 1
        },
        "reasoning": "`login` is the closest observed header for the canonical username field.",
        "reviewRequired": false
      }
    ],
    "confidenceNotes": [
      "The source file failed header validation, so each suggested mapping should be reviewed before reuse."
    ],
    "recommendedOperatorChecks": [
      "Confirm the source header order before editing any replay input."
    ],
    "promptVersion": "import-mapping-suggestion-v1",
    "modelId": "gpt-4.1-mini",
    "generatedAt": "2026-03-27T10:30:15",
    "latencyMs": 544,
    "requestId": "import-ai-mapping-suggestion-req-1"
  }
}
```

Current notes:

- requires `USER_READ`
- request body is empty
- the endpoint is read-only and suggestion-only; it does not mutate import jobs, item errors, source files, or replay state
- eligibility is intentionally narrow: the job must already have failure signal plus parseable sanitized header/global signal from existing `rowNumber=null` errors, otherwise it returns `400`
- the service sends normalized header tokens and bounded structural row summaries only; it does not rescan the source file or forward raw row values

### 27. Import AI Fix Recommendation (`POST /api/v1/import-jobs/{id}/ai-fix-recommendation`)

Response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "importJobId": 1201,
    "summary": "The import is mostly blocked by tenant role validation, with a smaller duplicate-username tail that should be handled as a separate cleanup step before replay.",
    "recommendedFixes": [
      {
        "errorCode": "UNKNOWN_ROLE",
        "recommendedAction": "Verify that the referenced role codes exist in the current tenant and normalize the source role-code format before preparing replay input.",
        "reasoning": "The grouped failures point to tenant role validation rather than CSV shape corruption.",
        "reviewRequired": true,
        "affectedRowsEstimate": 7
      }
    ],
    "confidenceNotes": [
      "The recommendations are grounded in row-level error groups, so operators should still confirm tenant-specific business rules before reuse."
    ],
    "recommendedOperatorChecks": [
      "Confirm which error-code group is the highest-volume cleanup target before editing replay input."
    ],
    "promptVersion": "import-fix-recommendation-v1",
    "modelId": "gpt-4.1-mini",
    "generatedAt": "2026-03-28T11:20:15",
    "latencyMs": 548,
    "requestId": "import-ai-fix-recommendation-req-1"
  }
}
```

Current notes:

- requires `USER_READ`
- request body is empty
- the endpoint is read-only and suggestion-only; it does not mutate import jobs, item errors, source files, or replay state
- eligibility is intentionally narrow: the job must already have failure signal, must be `USER_CSV`, and must expose grounded sanitized row-level error groups, otherwise it returns `400`
- the service sends grounded `errorCode` groups and bounded structural row summaries only; it does not rescan the source file, forward raw row values, or return direct replacement values
- provider output is validated locally and is rejected as `INVALID_RESPONSE` when it echoes raw CSV-like strings or sensitive local row values

## Stale Swagger Troubleshooting

- Confirm the process you are accessing is running on `http://localhost:8080`
- Refresh `/v3/api-docs` directly and then reload Swagger UI
