# ADR-0011: Keep Workflow Logs Separate From Generic Audit Events

- Status: Accepted
- Date: 2026-03-11

## Context

MerchantOps SaaS has completed its first real workflow module in Week 3 through the tenant-scoped ticket workflow baseline.

That workflow already records domain-specific events in `ticket_operation_log` for actions such as:

- ticket created
- assignee changed
- status changed
- comment added

Those events are useful because they read like workflow history and fit the ticket detail experience directly.

Week 4 now needs a broader governance layer that can support:

- reusable audit recording across user and ticket writes
- later approval / reject / rollback patterns
- AI-related traceability in future phases
- cross-entity operator and request tracing

If the project tries to replace workflow logs with one generic audit structure too early, two risks appear:

1. the ticket workflow loses a simple domain timeline that is easy to query and present
2. the generic audit model becomes overloaded with workflow-specific concerns before the Week 4 governance shape is stable

The repository therefore needs a clear rule for how workflow history and governance audit should coexist.

## Decision

MerchantOps SaaS will keep workflow-specific logs separate from generic audit events.

That means:

- preserve workflow-specific history such as `ticket_operation_log` for domain timelines and ticket detail playback
- add a separate generic audit-event model for reusable governance concerns across business modules
- allow a single write action to emit both a workflow log and a generic audit event when both are useful
- do not require the generic audit model to replace or reshape an existing workflow log API

Use the two layers for different purposes:

- workflow log:
  - domain-specific timeline
  - business-readable action detail
  - optimized for module detail pages and workflow playback
- generic audit event:
  - cross-module governance record
  - operator / request / entity traceability
  - future approval, rollback, and AI audit foundation

The generic audit event should be keyed by shared governance dimensions such as:

- `tenantId`
- `entityType`
- `entityId`
- `actionType`
- `operatorId`
- `requestId`
- `beforeValue`
- `afterValue`
- `approvalStatus`
- `createdAt`

The workflow log should remain free to keep domain-specific event names and detail text that are easier to read inside a workflow module.

## Consequences

- Week 4 can introduce a reusable `audit_event` backbone without breaking the completed Week 3 ticket contract
- ticket detail queries can continue to use workflow-level history without forcing UI consumers to read a generic governance schema
- user-management writes and ticket writes can converge on one shared audit foundation while still keeping different workflow histories where needed
- some event duplication is expected and intentional when a write operation needs both workflow playback and governance traceability
- future approval and AI features can depend on the generic audit layer instead of overloading ticket-only tables
- tests and documentation should verify the distinction clearly: workflow history remains module-facing, while generic audit is governance-facing
