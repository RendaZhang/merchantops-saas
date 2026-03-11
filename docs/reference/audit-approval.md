# Audit And Approval Patterns

## Current Week 4 Slice A Scope

As of `v0.1.2` baseline + current Week 4 Slice A implementation, the repository now includes a **generic audit backbone** for existing public write operations.

Implemented now:

- new `audit_event` table for tenant-scoped governance events
- DB-level same-tenant operator linkage for `audit_event (operator_id, tenant_id) -> users (id, tenant_id)`
- explicit `AuditEventService` with explicit `tenantId`, `operatorId`, and `requestId` inputs
- user write operations emit generic `audit_event` rows (`create`, `profile update`, `status update`, `role assignment`)
- ticket write operations emit generic `audit_event` rows (`create`, `assignee`, `status`, `comment`)
- ticket workflow-level `ticket_operation_log` remains in place and is **not** replaced by generic audit
- minimal tenant-scoped audit query endpoint: `GET /api/v1/audit-events?entityType=...&entityId=...`

## Public API

### `GET /api/v1/audit-events`

- scope: current tenant only (tenant derived from authenticated context)
- permission: `USER_READ`
- required query params:
  - `entityType` (case-insensitive; normalized to upper-case internally)
  - `entityId`
- returns audit rows ordered by insert id asc
- current public entity families emitted by existing writes: `USER` and `TICKET`
- current read shape is entity-scoped only; there is no pagination or broader search API yet

Example:

```http
GET /api/v1/audit-events?entityType=TICKET&entityId=402
Authorization: Bearer <token>
```

Example response:

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

## Data Model (Current Minimal)

`audit_event` fields:

- `tenant_id`
- `entity_type`
- `entity_id`
- `action_type`
- `operator_id`
- `request_id`
- `before_value`
- `after_value`
- `approval_status`
- `created_at`

`approval_status` is currently set to `NOT_REQUIRED` for this Slice A baseline.

Current payload note:

- `before_value` and `after_value` currently store lightweight JSON snapshots as strings, not a structured diff model
- `operator_id` is now constrained together with `tenant_id`, so the database rejects cross-tenant operator attribution for audit rows

## Explicit Non-Goals In Slice A

- no generic approval workflow tables yet
- no full “proposal -> approval -> execution” orchestration yet
- no generic diff engine yet (current snapshots use minimal JSON before/after payloads)

These stay in Week 4 Slice B+ work.
