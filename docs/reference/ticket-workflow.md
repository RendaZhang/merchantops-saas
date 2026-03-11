# Ticket Workflow

Last updated: 2026-03-12

## Public API Surface

Swagger currently exposes six ticket-workflow endpoints:

| Method | Path | Auth | Permission | Notes |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/tickets` | Bearer JWT | `TICKET_READ` | Pages tickets in the current tenant |
| `GET` | `/api/v1/tickets/{id}` | Bearer JWT | `TICKET_READ` | Returns one current-tenant ticket with comments and workflow logs |
| `POST` | `/api/v1/tickets` | Bearer JWT | `TICKET_WRITE` | Creates a new `OPEN` ticket |
| `PATCH` | `/api/v1/tickets/{id}/assignee` | Bearer JWT | `TICKET_WRITE` | Replaces the current assignee with an active tenant user |
| `PATCH` | `/api/v1/tickets/{id}/status` | Bearer JWT | `TICKET_WRITE` | Transitions the ticket state |
| `POST` | `/api/v1/tickets/{id}/comments` | Bearer JWT | `TICKET_WRITE` | Adds a comment and writes a workflow log entry |

Use Swagger UI or [../../api-demo.http](../../api-demo.http) for the current request flow.

## Minimal Workflow Model

Current Week 3 Slice A intentionally keeps the model narrow:

- statuses: `OPEN`, `IN_PROGRESS`, `CLOSED`
- write permissions: `TICKET_WRITE`
- read permission: `TICKET_READ`
- workflow log event types: `CREATED`, `ASSIGNED`, `STATUS_CHANGED`, `COMMENTED`

Current transition rules:

- `OPEN -> IN_PROGRESS`
- `OPEN -> CLOSED`
- `IN_PROGRESS -> CLOSED`

Rejected transitions today:

- any no-op such as `OPEN -> OPEN`
- any reopen attempt such as `CLOSED -> OPEN`
- any transition from `CLOSED` to another status

## `GET /api/v1/tickets`

Current behavior:

- tenant scope is derived from JWT and request context, not request parameters
- requires `TICKET_READ`, so seeded `admin`, `ops`, and `viewer` can all read their own tenant tickets
- supports:
  - `page` with default `0`
  - `size` with default `10`
  - `status` as exact filter
  - `assigneeId` as exact current assignee filter
  - `keyword` fuzzy filter against `title` and `description`
  - `unassignedOnly=true` to return only tickets with `assigneeId = null`
- rejects `assigneeId` combined with `unassignedOnly=true` with `BAD_REQUEST`
- response is a page object with `items`, `page`, `size`, `total`, and `totalPages`
- items are ordered by `updatedAt DESC`, then `id DESC`

Example request:

```text
GET /api/v1/tickets?page=0&size=10&status=OPEN&keyword=printer&unassignedOnly=true
```

Response example:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "items": [
      {
        "id": 301,
        "title": "POS printer offline",
        "status": "OPEN",
        "assigneeId": null,
        "assigneeUsername": null,
        "createdAt": "2026-03-11T09:00:00",
        "updatedAt": "2026-03-11T09:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "total": 1,
    "totalPages": 1
  }
}
```

## `GET /api/v1/tickets/{id}`

Current behavior:

- tenant scope is derived from JWT and request context
- requires `TICKET_READ`
- returns one current-tenant ticket only
- response includes core ticket fields plus:
  - `comments`
  - `operationLogs`
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

## `POST /api/v1/tickets`

Current behavior:

- requires `TICKET_WRITE`
- seeded `admin` and `ops` can create tickets
- seeded `viewer` cannot create tickets until a role reassignment gives `TICKET_WRITE`
- request fields:
  - `title`
  - `description`
- new tickets are always created with status `OPEN`
- the current request's `X-Request-Id` is stored internally on the ticket row and on the `CREATED` workflow log

Example request:

```json
{
  "title": "POS register frozen",
  "description": "Register screen froze during checkout."
}
```

Response example:

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

## `PATCH /api/v1/tickets/{id}/assignee`

Current behavior:

- requires `TICKET_WRITE`
- request body accepts `assigneeId` only
- assignee must:
  - exist in the current tenant
  - be `ACTIVE`
- writes an `ASSIGNED` workflow log entry

Example request:

```json
{
  "assigneeId": 2
}
```

Failure examples:

- cross-tenant assignee: `BAD_REQUEST`, message `user must exist in current tenant`
- disabled assignee: `BAD_REQUEST`, message `assignee must be active`

## `PATCH /api/v1/tickets/{id}/status`

Current behavior:

- requires `TICKET_WRITE`
- request body accepts only `OPEN`, `IN_PROGRESS`, or `CLOSED`
- transition validation is enforced in service logic
- closing a ticket is the same endpoint with `status = CLOSED`
- writes a `STATUS_CHANGED` workflow log entry

Example request:

```json
{
  "status": "CLOSED"
}
```

Failure example:

- invalid transition such as `CLOSED -> OPEN`: `BAD_REQUEST`, message `ticket status transition is not allowed`

## `POST /api/v1/tickets/{id}/comments`

Current behavior:

- requires `TICKET_WRITE`
- request body accepts `content`
- comment author is always the current operator from authenticated context
- writes both:
  - one `ticket_comment` row
  - one `COMMENTED` workflow log entry

Example request:

```json
{
  "content": "Investigating store terminal logs."
}
```

## Internal Tracking Notes

Week 3 Slice A keeps the tracking lightweight but explicit:

- controllers resolve `tenantId`, `operatorId`, and `requestId`
- lower layers do not read request-scoped state implicitly
- ticket writes persist workflow events into `ticket_operation_log`
- the public API does not expose raw `requestId`, but the database stores it for future audit work

This is intentionally a workflow-level log, not the full Week 4 generic audit subsystem.

## Demo Roles

Current seeded ticket permissions:

- `TENANT_ADMIN`: `TICKET_READ`, `TICKET_WRITE`
- `OPS_USER`: `TICKET_READ`, `TICKET_WRITE`
- `READ_ONLY`: `TICKET_READ`

That means:

- `admin` can create, assign, update status, and comment
- `ops` can create, assign, update status, and comment
- `viewer` can list and view details, but cannot write

If `viewer` is promoted through `PUT /api/v1/users/{id}/roles`, old JWT claims become stale immediately. The user must log in again before the new ticket permissions apply.

## Verification References

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- [api-docs.md](api-docs.md): endpoint coverage matrix
- [authentication-and-rbac.md](authentication-and-rbac.md): JWT and permission expectations
- [../runbooks/automated-tests.md](../runbooks/automated-tests.md): automated coverage
- [../runbooks/local-smoke-test.md](../runbooks/local-smoke-test.md): live verification flow
- [../runbooks/regression-checklist.md](../runbooks/regression-checklist.md): broader checklist
