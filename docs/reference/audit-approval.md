# Audit And Approval Patterns

## Current Week 4 Scope

As of `v0.1.2` baseline + current Week 4 Slice A/B/C implementation, the repository now includes a generic `audit_event` backbone plus a minimal approval envelope for one high-value action (`USER_STATUS_DISABLE`) and a queue read surface.

Implemented now:

- tenant-scoped `audit_event` write/read baseline, including `GET /api/v1/audit-events`
- tenant-scoped `approval_request` table for `USER_STATUS_DISABLE`
- user disable request flow:
  - `POST /api/v1/users/{id}/disable-requests`
  - `GET /api/v1/approval-requests`
  - `GET /api/v1/approval-requests/{id}`
  - `POST /api/v1/approval-requests/{id}/approve`
  - `POST /api/v1/approval-requests/{id}/reject`
- approve executes synchronously via existing user status write chain (`UserCommandService.updateStatus`)
- approval governance minimum rules:
  - tenant isolation on read/review/execute
  - only `PENDING` request can be approved/rejected
  - requester cannot self-approve/reject
  - approve-time revalidation that target user is still in current tenant and still `ACTIVE`

## Public API

### `GET /api/v1/audit-events`

- scope: current tenant only
- permission: `USER_READ`
- required query params: `entityType`, `entityId`
- still entity-scoped read only (no global/paged audit search yet)
- current entity families emitted by public flows: `USER`, `TICKET`, and `APPROVAL_REQUEST`

### `POST /api/v1/users/{id}/disable-requests`

- scope: current tenant only
- permission: `USER_WRITE`
- creates a `PENDING` approval request for `USER_STATUS_DISABLE`
- does not mutate target user status yet
- rejects duplicate pending disable requests for the same tenant user

### `GET /api/v1/approval-requests`

- scope: current tenant only
- permission: `USER_READ`
- supports `page`, `size`, `status`, `actionType`, `requestedBy` filters
- default sort: `createdAt DESC, id DESC` for stable queue ordering

### `GET /api/v1/approval-requests/{id}`

- scope: current tenant only
- permission: `USER_READ`

### `POST /api/v1/approval-requests/{id}/approve`

- scope: current tenant only
- permission: `USER_WRITE`
- transitions `PENDING -> APPROVED`
- executes target user status update to `DISABLED` in the same transaction boundary

### `POST /api/v1/approval-requests/{id}/reject`

- scope: current tenant only
- permission: `USER_WRITE`
- transitions `PENDING -> REJECTED`
- does not mutate target user status

Shared response shape example:

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

Current status values:

- `PENDING`
- `APPROVED`
- `REJECTED`

## Data Model

`approval_request` fields:

- `tenant_id`
- `action_type`
- `entity_type`
- `entity_id`
- `requested_by`
- `reviewed_by`
- `status`
- `payload_json`
- `request_id`
- `created_at`
- `reviewed_at`
- `executed_at`

Current constraint note:

- `requested_by` and `reviewed_by` both use same-tenant foreign-key linkage through `(user_id, tenant_id)` so cross-tenant reviewer attribution is rejected at the database layer

## Current Audit Coverage For Approval Flow

Week 4 minimal approval flow writes at least:

- `APPROVAL_REQUEST_CREATED`
- `APPROVAL_REQUEST_APPROVED` or `APPROVAL_REQUEST_REJECTED`
- `APPROVAL_ACTION_EXECUTED` (for approve path)
- existing user-chain event `USER_STATUS_UPDATED` from the reused write service

Current manual verification hint:

- query `GET /api/v1/audit-events?entityType=APPROVAL_REQUEST&entityId=<approvalRequestId>` to inspect approval-request lifecycle audit rows

## Still Planned (Not Yet Implemented)

- multi-action generic approval routing beyond `USER_STATUS_DISABLE`
- broader cross-entity audit/approval read surfaces (global search/aggregation statistics)
- async approval executors or delayed execution modes
