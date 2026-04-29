# Access Control Evolution Plan

This page defines the long-term access-control and authorization strategy from the `v0.7.0-beta` foundation baseline. It is a planning document, not a public API contract.

## Current Baseline

- Authentication is JWT-based and tenant-scoped.
- Endpoint permissions are enforced through explicit permission requirements.
- Protected requests re-check current tenant status, user status, roles, and permissions so stale claims are rejected until re-login.
- Approval queue, detail, approve, and reject behavior is action-aware rather than controller-wide.
- Feature-flag management is tenant-scoped and requires `FEATURE_FLAG_MANAGE`.
- `user_role` now has a database-level same-tenant invariant: every binding must match both `users.tenant_id` and `role.tenant_id`.
- Root ticket actors now have database-level same-tenant invariants: `ticket.assignee_id` and `ticket.created_by` must match `ticket.tenant_id` when they reference `users`.

Reference sources:

- [Authentication and RBAC](../reference/authentication-and-rbac.md)
- [User Management](../reference/user-management.md)
- [Audit and Approval](../reference/audit-approval.md)
- [Feature Flags](../reference/feature-flags.md)
- [Non-Blocking Backlog](non-blocking-backlog.md)

## Long-Term Priorities

### Database-Level Tenant Integrity

- Keep the resolved `user_role` same-tenant invariant covered by migrations, fixtures, and regression tests before widening role-management capabilities.
- Keep the resolved root ticket assignee/creator same-tenant invariant covered by migrations, fixtures, and regression tests before widening ticket actor surfaces.
- Add remaining database-level same-tenant constraints for ticket comment authors, operation-log operators, or child-table ticket relationships where service logic already enforces the boundary.
- Keep migrations narrow, backfilled, and covered by integration tests before making tenant-admin surfaces broader.

### RBAC Surface Productization

- Retire or de-emphasize `/api/v1/rbac/**` demo endpoints from the public story.
- Prefer business-oriented authorization examples through user, ticket, import, approval, and feature-flag workflows.
- Keep Swagger-visible permission examples aligned with real product workflows rather than verification-only helpers.

### Permission Taxonomy

- Review whether coarse permissions such as `USER_READ`, `USER_WRITE`, `TICKET_READ`, and `TICKET_WRITE` remain sufficient as workflow depth grows.
- Split permissions only when a concrete workflow needs separate read, propose, approve, execute, or manage boundaries.
- Avoid adding generic role-management complexity before the tenant-integrity layer is stronger.

### Action-Aware Approval Authorization

- Keep mixed-action approval routing tied to the approval action capability.
- Require new approval action types to define queue visibility, detail visibility, review authority, self-review behavior, duplicate-pending semantics, and audit evidence.
- Do not fall back to broad controller-level permissions for approval surfaces.

### Tenant Admin And Self-Service

- Treat tenant admin configuration as a later product track, not a default platform expansion.
- Candidate areas include role assignment UX, feature-flag administration, approval policy visibility, and operator audit review.
- Require clear permission boundaries before exposing broader tenant self-service configuration.

### Audit Evidence

- Keep authorization-sensitive writes tied to request identity, actor identity, and stable audit records.
- Expand audit snapshots only when a workflow needs reviewable evidence; avoid logging sensitive payloads by default.
- Keep AI-assisted execution bridges auditable from proposal through approval and execution.

## Planning Rules

- Use this page when access-control strategy changes.
- Use reference docs for current public behavior.
- Use [non-blocking-backlog.md](non-blocking-backlog.md) for concrete follow-up items that should remain visible but are not current blockers.
- Update [../product-strategy.md](../product-strategy.md) when access-control work changes the broader product direction.
