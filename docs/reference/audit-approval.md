# Audit And Approval Patterns

## Current Scope

The repository includes a generic `audit_event` backbone plus a still-narrow approval envelope for two high-value actions: `USER_STATUS_DISABLE` and `IMPORT_JOB_SELECTIVE_REPLAY`.

Implemented now:

- tenant-scoped `audit_event` write/read baseline, including `GET /api/v1/audit-events`
- tenant-scoped `approval_request` table for `USER_STATUS_DISABLE` and `IMPORT_JOB_SELECTIVE_REPLAY`
- user disable request flow:
  - `POST /api/v1/users/{id}/disable-requests`
  - `GET /api/v1/approval-requests`
  - `GET /api/v1/approval-requests/{id}`
  - `POST /api/v1/approval-requests/{id}/approve`
  - `POST /api/v1/approval-requests/{id}/reject`
- import selective replay proposal flow:
  - `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals`
  - `GET /api/v1/approval-requests`
  - `GET /api/v1/approval-requests/{id}`
  - `POST /api/v1/approval-requests/{id}/approve`
  - `POST /api/v1/approval-requests/{id}/reject`
- approve executes synchronously via the existing user status write chain (`UserCommandService.updateStatus`) for `USER_STATUS_DISABLE` and via the existing selective import replay chain for `IMPORT_JOB_SELECTIVE_REPLAY`

Week 6 also adds a separate `ai_interaction_record` model for AI runtime traceability. That model is not part of the public `GET /api/v1/audit-events` contract and does not replace `audit_event`.

## Public API

### `GET /api/v1/audit-events`

- scope: current tenant only
- permission: `USER_READ`
- required query params: `entityType`, `entityId`
- still entity-scoped read only; there is no global or paged audit search yet
- current entity families emitted by public write flows: `USER`, `TICKET`, `APPROVAL_REQUEST`, and `IMPORT_JOB`

### `POST /api/v1/users/{id}/disable-requests`

- scope: current tenant only
- permission: `USER_WRITE`
- creates a `PENDING` approval request for `USER_STATUS_DISABLE`
- does not mutate target user status yet
- rejects duplicate pending disable requests for the same tenant user

### `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals`

- scope: current tenant only
- permission: `USER_WRITE`
- creates a `PENDING` approval request for `IMPORT_JOB_SELECTIVE_REPLAY`
- proposal payload stores only `sourceJobId`, normalized `errorCodes`, optional `sourceInteractionId`, and optional `proposalReason`
- the endpoint does not create a replay job yet and does not persist raw CSV values or replay replacement values in `payload_json`
- `sourceInteractionId`, when present, must reference a same-job `FIX_RECOMMENDATION` interaction in `SUCCEEDED` status

### `GET /api/v1/approval-requests`

- scope: current tenant only
- permission: `USER_READ`
- supports `page`, `size`, `status`, `actionType`, and `requestedBy` filters
- default sort: `createdAt DESC, id DESC` for stable queue ordering

### `GET /api/v1/approval-requests/{id}`

- scope: current tenant only
- permission: `USER_READ`

### `POST /api/v1/approval-requests/{id}/approve`

- scope: current tenant only
- permission: `USER_WRITE`
- transitions `PENDING -> APPROVED`
- executes the underlying action in the same transaction boundary:
  - `USER_STATUS_DISABLE`: target user status update to `DISABLED`
  - `IMPORT_JOB_SELECTIVE_REPLAY`: existing selective replay execution, creating one new derived import job when approval succeeds

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

Current import selective replay payload note:

- `payload_json` is intentionally narrow and does not store raw CSV rows, replay replacement values, passwords, emails, or other sensitive replay inputs

Current constraint note:

- `requested_by` and `reviewed_by` both use same-tenant foreign-key linkage through `(user_id, tenant_id)` so cross-tenant reviewer attribution is rejected at the database layer

## Current Audit Coverage For Approval Flow

The current approval flow writes at least:

- `APPROVAL_REQUEST_CREATED`
- `APPROVAL_REQUEST_APPROVED` or `APPROVAL_REQUEST_REJECTED`
- `APPROVAL_ACTION_EXECUTED` (for approve path)
- existing user-chain event `USER_STATUS_UPDATED` from the reused write service
- existing import replay-chain events `IMPORT_JOB_REPLAY_REQUESTED` and `IMPORT_JOB_CREATED` when an `IMPORT_JOB_SELECTIVE_REPLAY` request is approved

Manual verification hint:

- query `GET /api/v1/audit-events?entityType=APPROVAL_REQUEST&entityId=<approvalRequestId>` to inspect approval-request lifecycle audit rows

## AI Traceability Note

Week 6 summary calls are intentionally recorded outside `audit_event`:

- `audit_event` remains for business-governance snapshots around write actions
- `ticket_operation_log` remains the domain-readable ticket timeline
- `ai_interaction_record` captures AI runtime metadata such as `promptVersion`, `modelId`, `status`, latency, and optional usage fields

This separation keeps read-only AI suggestion calls from polluting the business audit stream while still preserving traceability.

## Still Planned (Not Yet Implemented)

- broader multi-action approval routing beyond the current `USER_STATUS_DISABLE` and `IMPORT_JOB_SELECTIVE_REPLAY` pair
- broader cross-entity audit and approval read surfaces
- public read surfaces for AI interaction history or usage summaries
- async approval executors or delayed execution modes
