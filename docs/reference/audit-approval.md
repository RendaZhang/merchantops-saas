# Audit And Approval Patterns

Last updated: 2026-04-06

## Current Scope

The repository includes a generic `audit_event` backbone plus a still-narrow approval envelope for three high-value actions: `USER_STATUS_DISABLE`, `IMPORT_JOB_SELECTIVE_REPLAY`, and `TICKET_COMMENT_CREATE`.

Implemented now:

- tenant-scoped `audit_event` write/read baseline, including `GET /api/v1/audit-events`
- tenant-scoped `approval_request` table for `USER_STATUS_DISABLE`, `IMPORT_JOB_SELECTIVE_REPLAY`, and `TICKET_COMMENT_CREATE`
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
- ticket reply-draft comment proposal flow:
  - `POST /api/v1/tickets/{id}/comments/proposals/ai-reply-draft`
  - `GET /api/v1/approval-requests`
  - `GET /api/v1/approval-requests/{id}`
  - `POST /api/v1/approval-requests/{id}/approve`
  - `POST /api/v1/approval-requests/{id}/reject`
- approve executes synchronously via the existing user status write chain (`UserCommandService.updateStatus`) for `USER_STATUS_DISABLE`, the existing selective import replay chain for `IMPORT_JOB_SELECTIVE_REPLAY`, and the existing ticket comment write chain for `TICKET_COMMENT_CREATE`
- the shared approval queue is now action-aware: read visibility and review permission are derived from the approval action rather than a controller-wide `USER_*` gate
- the completed Week 8 baseline now includes two separate proposal -> approval -> execution bridges plus shared pending-request-key hardening across all three shipped approval action types

Week 6 also adds a separate `ai_interaction_record` model for AI runtime traceability. That model is not part of the public `GET /api/v1/audit-events` contract and does not replace `audit_event`.

## Public API

### `GET /api/v1/audit-events`

- scope: current tenant only
- permission: `USER_READ`
- required query params: `entityType`, `entityId`
- still entity-scoped read only; there is no global or paged audit search yet
- current entity families emitted by public write flows: `USER`, `TICKET`, `APPROVAL_REQUEST`, `IMPORT_JOB`, and `FEATURE_FLAG`
- feature-flag updates currently emit `entityType=FEATURE_FLAG` and `actionType=FEATURE_FLAG_UPDATED` with before/after snapshots over the stored tenant-scoped flag row

### `POST /api/v1/users/{id}/disable-requests`

- scope: current tenant only
- permission: `USER_WRITE`
- creates a `PENDING` approval request for `USER_STATUS_DISABLE`
- does not mutate target user status yet
- rejects duplicate pending disable requests for the same tenant user
- duplicate suppression is now also enforced at the database layer through a derived pending-request key, so concurrent duplicate creates still collapse into the same public `400` behavior

### `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals`

- scope: current tenant only
- permission: `USER_WRITE`
- creates a `PENDING` approval request for `IMPORT_JOB_SELECTIVE_REPLAY`
- proposal payload stores only `sourceJobId`, canonical sorted `errorCodes`, optional `sourceInteractionId`, and optional `proposalReason`
- the endpoint does not create a replay job yet and does not persist raw CSV values or replay replacement values in `payload_json`
- `sourceInteractionId`, when present, must reference a same-job `FIX_RECOMMENDATION` interaction in `SUCCEEDED` status
- duplicate pending proposals for the same tenant, source job, and canonical `errorCodes` return `400`, message `pending selective replay proposal already exists for source job and selected errorCodes`
- `sourceInteractionId` and `proposalReason` are provenance only and do not affect pending-proposal uniqueness

### `POST /api/v1/tickets/{id}/comments/proposals/ai-reply-draft`

- scope: current tenant only
- permission: `TICKET_WRITE`
- creates a `PENDING` approval request for `TICKET_COMMENT_CREATE`
- proposal payload stores only trimmed `commentContent` and optional `sourceInteractionId`
- `sourceInteractionId`, when present, must reference a same-ticket `REPLY_DRAFT` interaction in `SUCCEEDED` status
- the endpoint does not create a comment yet and does not persist raw prompt text, raw provider payload, or model metadata in `payload_json`
- duplicate pending proposals for the same tenant, ticket, and trimmed `commentContent` return `400`, message `pending ticket comment proposal already exists for ticket and comment content`
- `sourceInteractionId` is provenance only and does not affect pending-proposal uniqueness

### `GET /api/v1/approval-requests`

- scope: current tenant only
- permission: action-aware approval read
- action mapping:
  - `USER_STATUS_DISABLE` -> `USER_READ`
  - `IMPORT_JOB_SELECTIVE_REPLAY` -> `USER_READ`
  - `TICKET_COMMENT_CREATE` -> `TICKET_READ`
- supports `page`, `size`, `status`, `actionType`, and `requestedBy` filters
- requires at least one readable action capability
- returns only the action types visible to the current caller
- explicit `actionType` filters outside the caller's visible action set return an empty page
- default sort: `createdAt DESC, id DESC` for stable queue ordering

### `GET /api/v1/approval-requests/{id}`

- scope: current tenant only
- permission: action-aware approval read
- hidden action types return `404` after same-tenant lookup so mixed queues do not leak other action families

### `POST /api/v1/approval-requests/{id}/approve`

- scope: current tenant only
- permission: action-aware approval review
- action mapping:
  - `USER_STATUS_DISABLE` -> `USER_WRITE`
  - `IMPORT_JOB_SELECTIVE_REPLAY` -> `USER_WRITE`
  - `TICKET_COMMENT_CREATE` -> `TICKET_WRITE`
- transitions `PENDING -> APPROVED`
- executes the underlying action in the same transaction boundary:
  - `USER_STATUS_DISABLE`: target user status update to `DISABLED`
  - `IMPORT_JOB_SELECTIVE_REPLAY`: existing selective replay execution, creating one new derived import job when approval succeeds
  - `TICKET_COMMENT_CREATE`: existing ticket comment execution, creating exactly one new comment plus the normal ticket workflow log and audit side effects
- requests that are already `APPROVED` or `REJECTED` return `400`, message `approval request is not pending`

### `POST /api/v1/approval-requests/{id}/reject`

- scope: current tenant only
- permission: action-aware approval review
- transitions `PENDING -> REJECTED`
- does not mutate target user status, create a replay job, or create a comment
- requests that are already `APPROVED` or `REJECTED` return `400`, message `approval request is not pending`

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
- `pending_request_key`
- `request_id`
- `created_at`
- `reviewed_at`
- `executed_at`

Current import selective replay payload note:

- `payload_json` is intentionally narrow and does not store raw CSV rows, replay replacement values, passwords, emails, or other sensitive replay inputs

Current ticket comment payload note:

- `payload_json` is intentionally narrow and does not store raw prompt text, raw provider payload, model metadata, or any AI runtime fields beyond optional `sourceInteractionId`

Current constraint note:

- `requested_by` and `reviewed_by` both use same-tenant foreign-key linkage through `(user_id, tenant_id)` so cross-tenant reviewer attribution is rejected at the database layer
- pending approval requests now share one action-aware uniqueness key across all three shipped actions:
  - `USER_STATUS_DISABLE:<tenantId>:<userId>`
  - `IMPORT_JOB_SELECTIVE_REPLAY:<tenantId>:<sourceJobId>:<md5(canonicalErrorCodes)>`
  - `TICKET_COMMENT_CREATE:<tenantId>:<ticketId>:<md5(trimmedCommentContent)>`
- import proposal uniqueness is based on executable payload only, so `sourceInteractionId` and `proposalReason` do not affect the pending key
- ticket comment proposal uniqueness is based on executable payload only, so `sourceInteractionId` does not affect the pending key
- review resolution clears `pending_request_key`, so the same executable payload can be proposed again after `APPROVED` or `REJECTED`

## Current Audit Coverage For Approval Flow

The current approval flow writes at least:

- `APPROVAL_REQUEST_CREATED`
- `APPROVAL_REQUEST_APPROVED` or `APPROVAL_REQUEST_REJECTED`
- `APPROVAL_ACTION_EXECUTED` (for approve path)
- existing user-chain event `USER_STATUS_UPDATED` from the reused write service
- existing import replay-chain events `IMPORT_JOB_REPLAY_REQUESTED` and `IMPORT_JOB_CREATED` when an `IMPORT_JOB_SELECTIVE_REPLAY` request is approved
- existing ticket comment-chain workflow-log and audit side effects when a `TICKET_COMMENT_CREATE` request is approved

Manual verification hint:

- query `GET /api/v1/audit-events?entityType=APPROVAL_REQUEST&entityId=<approvalRequestId>` to inspect approval-request lifecycle audit rows

## AI Traceability Note

Week 6-8 AI suggestion calls are intentionally recorded outside `audit_event`:

- `audit_event` remains for business-governance snapshots around write actions
- `ticket_operation_log` remains the domain-readable ticket timeline
- `ai_interaction_record` captures AI runtime metadata such as `promptVersion`, `modelId`, `status`, latency, and optional usage fields

This separation keeps read-only AI suggestion calls from polluting the business audit stream while still preserving traceability.

## Still Planned (Not Yet Implemented)

- broader multi-action approval routing beyond the current `USER_STATUS_DISABLE`, `IMPORT_JOB_SELECTIVE_REPLAY`, and `TICKET_COMMENT_CREATE` trio
- broader cross-entity audit and approval read surfaces
- public aggregate AI usage summary surfaces
- async approval executors or delayed execution modes
