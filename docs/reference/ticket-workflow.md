# Ticket Workflow

Last updated: 2026-03-21

## Public API Surface

Swagger currently exposes eight ticket-workflow endpoints:

| Method | Path | Auth | Permission | Notes |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/tickets` | Bearer JWT | `TICKET_READ` | Pages tickets in the current tenant |
| `GET` | `/api/v1/tickets/{id}` | Bearer JWT | `TICKET_READ` | Returns one current-tenant ticket with comments and workflow logs |
| `POST` | `/api/v1/tickets/{id}/ai-summary` | Bearer JWT | `TICKET_READ` | Generates a suggestion-only AI summary from the current ticket detail context |
| `POST` | `/api/v1/tickets/{id}/ai-triage` | Bearer JWT | `TICKET_READ` | Generates suggestion-only AI classification and priority guidance from the current ticket detail context |
| `POST` | `/api/v1/tickets` | Bearer JWT | `TICKET_WRITE` | Creates a new `OPEN` ticket |
| `PATCH` | `/api/v1/tickets/{id}/assignee` | Bearer JWT | `TICKET_WRITE` | Replaces the current assignee with an active tenant user |
| `PATCH` | `/api/v1/tickets/{id}/status` | Bearer JWT | `TICKET_WRITE` | Transitions the ticket state |
| `POST` | `/api/v1/tickets/{id}/comments` | Bearer JWT | `TICKET_WRITE` | Adds a comment and writes a workflow log entry |

Use Swagger UI or [../../api-demo.http](../../api-demo.http) for the current request flow.

## Minimal Workflow Model

Current Week 3-6 ticket workflow keeps the business state model narrow:

- statuses: `OPEN`, `IN_PROGRESS`, `CLOSED`
- write permission: `TICKET_WRITE`
- read permission: `TICKET_READ`
- workflow log event types: `CREATED`, `ASSIGNED`, `STATUS_CHANGED`, `COMMENTED`
- AI summary and triage behavior: suggestion-only, read-only, and non-mutating

Current transition rules:

- `OPEN -> IN_PROGRESS`
- `OPEN -> CLOSED`
- `IN_PROGRESS -> CLOSED`
- `CLOSED -> OPEN` (reopen)

Rejected transitions today:

- any no-op such as `OPEN -> OPEN`
- any no-op reopen request such as `CLOSED -> CLOSED`
- any transition that is not one of the allowed rules above

## `GET /api/v1/tickets`

Current behavior:

- tenant scope is derived from JWT and request context, not request parameters
- requires `TICKET_READ`, so seeded `admin`, `ops`, and `viewer` can all read their own tenant tickets
- supports `page`, `size`, `status`, `assigneeId`, `keyword`, and `unassignedOnly=true`
- rejects `assigneeId` combined with `unassignedOnly=true` with `BAD_REQUEST`
- returns a page object ordered by `updatedAt DESC`, then `id DESC`

## `GET /api/v1/tickets/{id}`

Current behavior:

- tenant scope is derived from JWT and request context
- requires `TICKET_READ`
- returns one current-tenant ticket only
- response includes core ticket fields plus `comments` and `operationLogs`
- returns `404` if the ticket is outside the current tenant

Response example:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "id": 302,
    "tenantId": 1,
    "title": "Printer cable replacement",
    "description": "Replace damaged cable for the store printer.",
    "status": "IN_PROGRESS",
    "assigneeId": 2,
    "assigneeUsername": "ops",
    "createdBy": 1,
    "createdByUsername": "admin",
    "createdAt": "2026-03-11T09:30:00",
    "updatedAt": "2026-03-11T10:20:00",
    "comments": [
      {
        "id": 31,
        "ticketId": 302,
        "content": "Cable swap started.",
        "createdBy": 2,
        "createdByUsername": "ops",
        "createdAt": "2026-03-11T10:05:00"
      }
    ],
    "operationLogs": [
      {
        "id": 41,
        "operationType": "CREATED",
        "detail": "ticket created",
        "operatorId": 1,
        "operatorUsername": "admin",
        "createdAt": "2026-03-11T09:30:00"
      }
    ]
  }
}
```

## `POST /api/v1/tickets/{id}/ai-summary`

Current behavior:

- requires `TICKET_READ`
- loads the target ticket through the existing tenant-scoped detail query path
- builds the prompt from ticket core fields, comments, and workflow logs only
- returns a suggestion-only summary and does not mutate ticket state, write comments, or trigger approvals
- returns controlled `503 SERVICE_UNAVAILABLE` responses when AI is disabled, not configured, or unavailable
- writes an internal `ai_interaction_record` row for success and controlled failure states

Response example:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "ticketId": 302,
    "summary": "Issue: Printer cable replacement is in progress under ops. Current: the ticket is assigned and the latest signal says cable swap started. Next: confirm the replacement outcome and close the ticket if the printer is healthy.",
    "promptVersion": "ticket-summary-v1",
    "modelId": "gpt-4.1-mini",
    "generatedAt": "2026-03-19T13:20:15",
    "latencyMs": 412,
    "requestId": "ticket-ai-summary-req-1"
  }
}
```

Failure examples:

- missing `TICKET_READ`: `403`, message `permission denied`
- ticket outside the current tenant: `404`, message `ticket not found`
- AI disabled: `503`, message `ticket ai summary is disabled`
- provider unavailable: `503`, message `ticket ai summary is unavailable`

## `POST /api/v1/tickets/{id}/ai-triage`

Current behavior:

- requires `TICKET_READ`
- loads the target ticket through the existing tenant-scoped detail query path
- builds the prompt from ticket core fields, comments, and workflow logs only
- returns a suggestion-only classification, priority, and reasoning result
- does not mutate ticket state, write comments, trigger approvals, or suggest assignees
- returns controlled `503 SERVICE_UNAVAILABLE` responses when AI is disabled, not configured, or unavailable
- writes an internal `ai_interaction_record` row for success and controlled failure states

Response example:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "ticketId": 302,
    "classification": "DEVICE_ISSUE",
    "priority": "HIGH",
    "reasoning": "The ticket describes a store printer issue affecting active operations and the latest signal still points to an unfinished hardware fix, so it should be treated as a high-priority device issue.",
    "promptVersion": "ticket-triage-v1",
    "modelId": "gpt-4.1-mini",
    "generatedAt": "2026-03-21T14:20:15",
    "latencyMs": 418,
    "requestId": "ticket-ai-triage-req-1"
  }
}
```

Failure examples:

- missing `TICKET_READ`: `403`, message `permission denied`
- ticket outside the current tenant: `404`, message `ticket not found`
- AI disabled: `503`, message `ticket ai triage is disabled`
- provider unavailable: `503`, message `ticket ai triage is unavailable`

## `POST /api/v1/tickets`

Current behavior:

- requires `TICKET_WRITE`
- seeded `admin` and `ops` can create tickets
- seeded `viewer` cannot create tickets until a role reassignment gives `TICKET_WRITE`
- request fields: `title`, `description`
- new tickets are always created with status `OPEN`
- the current request's `X-Request-Id` is stored internally on the ticket row and on the `CREATED` workflow log

## `PATCH /api/v1/tickets/{id}/assignee`

Current behavior:

- requires `TICKET_WRITE`
- request body accepts `assigneeId` only
- assignee must exist in the current tenant and be `ACTIVE`
- writes an `ASSIGNED` workflow log entry

Failure examples:

- cross-tenant assignee: `BAD_REQUEST`, message `user must exist in current tenant`
- disabled assignee: `BAD_REQUEST`, message `assignee must be active`

## `PATCH /api/v1/tickets/{id}/status`

Current behavior:

- requires `TICKET_WRITE`
- request body accepts only `OPEN`, `IN_PROGRESS`, or `CLOSED`
- transition validation is enforced in service logic
- no-op requests are rejected before transition validation and return `BAD_REQUEST` with the current status in the message
- writes a `STATUS_CHANGED` workflow log entry

Failure examples:

- invalid transition such as `IN_PROGRESS -> OPEN`: `BAD_REQUEST`, message `ticket status transition is not allowed`
- no-op transition such as `CLOSED -> CLOSED`: `BAD_REQUEST`, message `ticket is already in status CLOSED`

## `POST /api/v1/tickets/{id}/comments`

Current behavior:

- requires `TICKET_WRITE`
- request body accepts `content`
- comment author is always the current operator from authenticated context
- writes both one `ticket_comment` row and one `COMMENTED` workflow log entry

## Internal Tracking Notes

Current ticket-related tracking is intentionally split by concern:

- controllers resolve `tenantId`, `operatorId`, and `requestId`
- ticket writes persist workflow history into `ticket_operation_log`
- ticket writes also emit governance-oriented `audit_event` rows where applicable
- AI summary and triage calls persist runtime traceability into `ai_interaction_record`
- the public ticket and AI response shapes do not expose raw provider payloads or internal audit tables directly

## Demo Roles

Current seeded ticket permissions:

- `TENANT_ADMIN`: `TICKET_READ`, `TICKET_WRITE`
- `OPS_USER`: `TICKET_READ`, `TICKET_WRITE`
- `READ_ONLY`: `TICKET_READ`

That means:

- `admin` can create, assign, update status, comment, and request AI summaries or triage suggestions
- `ops` can create, assign, update status, comment, and request AI summaries or triage suggestions
- `viewer` can list, view details, and request AI summaries or triage suggestions, but cannot write

If `viewer` is promoted through `PUT /api/v1/users/{id}/roles`, old JWT claims become stale immediately. The user must log in again before the new ticket permissions apply.

## Verification References

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- [api-docs.md](api-docs.md): endpoint coverage matrix
- [authentication-and-rbac.md](authentication-and-rbac.md): JWT and permission expectations
- [ai-integration.md](ai-integration.md): AI summary and triage contracts plus runtime boundary
- [../runbooks/automated-tests.md](../runbooks/automated-tests.md): automated coverage
- [../runbooks/ai-regression-checklist.md](../runbooks/ai-regression-checklist.md): AI-specific regression checks
- [../runbooks/local-smoke-test.md](../runbooks/local-smoke-test.md): live verification flow
- [../runbooks/regression-checklist.md](../runbooks/regression-checklist.md): broader checklist
