# Feature Flags

Last updated: 2026-04-06

This page describes the current public tenant-scoped feature-flag surface.

Use it together with [api-docs.md](api-docs.md), [authentication-and-rbac.md](authentication-and-rbac.md), [ai-integration.md](ai-integration.md), [import-jobs.md](import-jobs.md), and [ticket-workflow.md](ticket-workflow.md) when rollout control, permission checks, or gated workflow behavior changes.

## Current Public Contract

The current public feature-flag API is:

- `GET /api/v1/feature-flags`
- `PUT /api/v1/feature-flags/{key}`

Current notes:

- both endpoints are visible in Swagger under the `Feature Flags` tag
- both endpoints require `FEATURE_FLAG_MANAGE` and operate on the authenticated user's current tenant
- the list response returns one fixed persisted flag set for the current tenant in stable `key ASC` order
- `PUT` accepts `{ "enabled": true|false }`
- `enabled` must not be `null`
- `PUT` returns `404` for an unknown key
- `PUT` is idempotent when the requested state is unchanged
- this API manages persisted rollout flags only; it does not manage config-level `merchantops.ai.enabled`

## Response Shape

### `GET /api/v1/feature-flags`

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "items": [
      {
        "id": 4,
        "key": "ai.import.error-summary.enabled",
        "enabled": true,
        "updatedAt": "2026-04-06T13:18:00"
      },
      {
        "id": 1,
        "key": "ai.ticket.summary.enabled",
        "enabled": true,
        "updatedAt": "2026-04-06T13:18:00"
      }
    ]
  }
}
```

Current notes:

- each row exposes `id`, `key`, `enabled`, and `updatedAt`
- the current list is fixed-key inventory for the authenticated user's current tenant, not user-defined custom flag storage
- the current API is tenant-scoped only; it does not expose cross-tenant admin, rollout percentage, environment-specific policy, or rule evaluation

### `PUT /api/v1/feature-flags/{key}`

Request:

```json
{
  "enabled": false
}
```

Response:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "id": 1,
    "key": "ai.ticket.summary.enabled",
    "enabled": false,
    "updatedAt": "2026-04-06T13:27:00"
  }
}
```

Current notes:

- updates one fixed persisted key only
- `enabled` must be a concrete boolean; `null` is rejected with `400 BAD_REQUEST`, message `enabled must not be null`
- leaves unrelated keys unchanged
- writes an audit row when the stored state changes
- returns the same stable item shape as the list endpoint
- idempotent short-circuit applies only when the requested boolean equals the current stored value

## Current Fixed Flag Set

The current persisted flag inventory contains exactly eight keys:

| Key | Current purpose |
| --- | --- |
| `ai.ticket.summary.enabled` | Gates `POST /api/v1/tickets/{id}/ai-summary` |
| `ai.ticket.triage.enabled` | Gates `POST /api/v1/tickets/{id}/ai-triage` |
| `ai.ticket.reply-draft.enabled` | Gates `POST /api/v1/tickets/{id}/ai-reply-draft` |
| `ai.import.error-summary.enabled` | Gates `POST /api/v1/import-jobs/{id}/ai-error-summary` |
| `ai.import.mapping-suggestion.enabled` | Gates `POST /api/v1/import-jobs/{id}/ai-mapping-suggestion` |
| `ai.import.fix-recommendation.enabled` | Gates `POST /api/v1/import-jobs/{id}/ai-fix-recommendation` |
| `workflow.import.selective-replay-proposal.enabled` | Gates `POST /api/v1/import-jobs/{id}/replay-failures/selective/proposals` |
| `workflow.ticket.comment-proposal.enabled` | Gates `POST /api/v1/tickets/{id}/comments/proposals/ai-reply-draft` |

Current notes:

- the six AI generation endpoints require both config-level `merchantops.ai.enabled=true` and their matching persisted flag
- the three public AI read endpoints are not gated by these persisted AI flags:
  - `GET /api/v1/tickets/{id}/ai-interactions`
  - `GET /api/v1/import-jobs/{id}/ai-interactions`
  - `GET /api/v1/ai-interactions/usage-summary`
- the two Week 8 workflow proposal bridges are controlled separately from the AI generation flags

## Disabled Behavior

When a matching persisted flag is disabled:

- the gated generation or proposal endpoint returns controlled `503 SERVICE_UNAVAILABLE`
- the disabled path stays explicit and non-silent
- unrelated AI endpoints and unrelated workflow bridges stay unaffected
- the disabled proposal path creates no approval row and no hidden fallback execution

This is rollout control, not a billing or policy engine.

## Permissions and Token Freshness

The current public permission boundary is:

- `FEATURE_FLAG_MANAGE` is required for both list and update on the current tenant's flag rows
- users who gain `FEATURE_FLAG_MANAGE` through a role change must login again before the new token can use the endpoints
- tokens issued before role or permission changes still fail on protected requests when claims become stale

See [authentication-and-rbac.md](authentication-and-rbac.md) for the broader stale-token and refreshed-login behavior.

## Audit Behavior

Feature-flag updates currently write audit rows with:

- `entityType=FEATURE_FLAG`
- `actionType=FEATURE_FLAG_UPDATED`
- before/after snapshots over the stored tenant-scoped flag row, including `tenantId`

Feature-flag changes do not reuse the AI interaction record model.

See [audit-approval.md](audit-approval.md) for the current audit-event baseline.

## Non-Goals

The current feature-flag surface is intentionally narrow:

- not a cross-tenant admin surface
- not percentage-based
- not environment-policy driven
- not user-segment aware
- not a generic rule engine
- not a replacement for deployment-time configuration such as `merchantops.ai.enabled`
