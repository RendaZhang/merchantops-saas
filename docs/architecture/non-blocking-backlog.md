# Non-Blocking Backlog

## Purpose

This page records real follow-up items that are important enough to stay visible, but do not block the current phase from being considered complete.

Use it for:

- schema-integrity gaps
- workflow hardening gaps
- productization follow-ups
- observability or governance follow-ups

Do not use it for:

- one-off bug notes
- temporary workarounds
- items that already block the current phase and should instead be reflected in `docs/project-status.md` or `docs/roadmap.md`

## How To Use This Page

- add an item when the gap is real, reusable, and likely to matter after the current task or phase ends
- keep each item short and scannable here; if an item grows too large, add a dedicated detail page later and link it back from the item
- if an item becomes phase-blocking, move that blocker status into `docs/project-status.md` or `docs/roadmap.md` instead of leaving it only here
- keep the wording phase-aware: record what it does not block now, not what it can never block later

## Item Template

Use this shape for future entries:

### NB-XXX: Short title

- State: Open
- Category: Schema Integrity / Workflow Hardening / Productization / Observability / AI Governance / Other
- Discovered in: Week X
- Not blocking: current phase or milestone name
- Priority: Low / Medium / High
- Recommended window: future phase or milestone
- Related docs: optional list of related pages

Current state:

Why it matters:

Why it is non-blocking now:

Recommended follow-up:

## Current Items

### NB-001: User-Role Database-Level Tenant Integrity

- State: Resolved in Productization Baseline Slice D
- Category: Schema Integrity
- Discovered in: Week 1
- Not blocking: Productization Baseline Slice E
- Priority: Medium
- Recommended window: completed by `V16__enforce_user_role_tenant_integrity.sql`
- Related docs: [../reference/database-migrations.md](../reference/database-migrations.md), [../project-status.md](../project-status.md)

Current state:

- `user_role` now carries `tenant_id`
- `V16__enforce_user_role_tenant_integrity.sql` backfills `tenant_id` from `users`, adds child indexes, and adds composite foreign keys to both `users(id, tenant_id)` and `role(id, tenant_id)`
- application logic still validates tenant-local role codes before writes; the database invariant is defense-in-depth

Why it matters:

- breaks tenant-isolation assumptions below the service layer
- can cause cross-tenant authorization leakage if bad rows are inserted directly
- makes incident repair and future audit work harder

Why it is non-blocking now:

- the gap has been resolved for `user_role`
- remaining actor-link schema integrity work is tracked separately under NB-002

Recommended follow-up:

- Keep the `user_role` invariant covered when role-management fixtures or migrations change.
- Continue ticket actor database-level tenant integrity as NB-002 rather than reopening `user_role` scope.

### NB-002: Ticket Actor Tenant Integrity At The Database Layer

- State: Open
- Category: Schema Integrity
- Discovered in: Week 3
- Not blocking: Week 3 completion or the current Week 4 work
- Priority: Medium
- Recommended window: Week 4 or early Week 5
- Related docs: [../reference/ticket-workflow.md](../reference/ticket-workflow.md), [../project-status.md](../project-status.md)

Current state:

- `ticket.assignee_id`, `ticket.created_by`, `ticket_comment.created_by`, and `ticket_operation_log.operator_id` currently reference `users.id`
- service logic already enforces same-tenant actor checks for current ticket workflows
- the database does not yet enforce `child.tenant_id == users.tenant_id` for those relationships

Why it matters:

- invalid cross-tenant actor links are still possible through direct data manipulation
- later audit, approval, and AI features will depend on trustworthy actor lineage

Why it is non-blocking now:

- the current public ticket workflow validates assignee and operator tenant scope in service logic
- automated coverage already exercises current-tenant assignment, write, reopen, and query behavior

Recommended follow-up:

1. add composite indexed keys such as `(id, tenant_id)` on `users`
2. add composite foreign keys that bind ticket-related user references to the same tenant
3. backfill and validate existing rows before enabling strict constraints
4. add integration tests that prove cross-tenant actor linkage fails below the service layer

### NB-003: RBAC Demo Endpoint Productionization Gap

- State: Open
- Category: Productization
- Discovered in: Week 1
- Not blocking: Week 3 completion or the current Week 4 work
- Priority: Medium
- Recommended window: before a `v0.2.0-alpha` style preview
- Related docs: [../reference/authentication-and-rbac.md](../reference/authentication-and-rbac.md), [../project-status.md](../project-status.md)

Current state:

- endpoints under `/api/v1/rbac/**` still exist as demo-oriented verification surfaces
- they are useful for permission checks and local verification, but they are not strong product-facing business APIs

Why it matters:

- they can confuse the public story about which APIs are real business workflows versus verification helpers
- later open-source packaging should rely more on business-oriented examples than demo-only endpoints

Why it is non-blocking now:

- current public business workflows already rely on `@RequirePermission` and real business endpoints under `/api/v1/users` and `/api/v1/tickets`
- Week 3 acceptance did not depend on replacing or deleting the demo endpoints

Recommended follow-up:

1. retire these endpoints from the public-facing story before the first broader open-source preview
2. or replace them with clearer business-oriented verification guidance and more representative example flows

## Current Execution Guidance

- start the current phase without waiting for every item here to be fixed
- keep these items visible in architecture notes instead of losing them in chat history
- implement them as narrow follow-up slices that do not silently reopen already-completed public contracts unless necessary


### NB-004: Generic Audit Backbone Hardening Follow-ups

- State: In Progress
- Category: Governance Hardening
- Discovered in: Week 4 Slice A
- Not blocking: Week 4 Slice A completion
- Priority: Medium

Current state:

- generic `audit_event` backbone is now implemented for current public write paths
- current before/after snapshots intentionally capture minimal key fields, not full domain diffs

Recommended follow-up:

1. define per-entity key-field snapshot policy and redaction policy
2. add pagination/sorting/filter extensions for audit queries when needed
3. add approval tables/flow in Slice B without weakening explicit context propagation
