# Tenant RBAC Integrity Gap (Recorded, Not Fixed Yet)

## Status

- State: Open
- Priority: Medium
- Decision: Record now, fix in a later migration
- Recorded on: 2026-03-08

## Context

Current RBAC tables:

- `tenant`
- `users` (`tenant_id` FK -> `tenant.id`)
- `role` (`tenant_id` FK -> `tenant.id`)
- `user_role` (`user_id` FK -> `users.id`, `role_id` FK -> `role.id`)

Both `users` and `role` are tenant-scoped, which is correct for multi-tenant design.

## Identified Gap

`user_role` does not enforce `users.tenant_id == role.tenant_id` at database level.

That means the database currently allows a user from tenant A to be linked to a role from tenant B, if such data is inserted.

## Why This Matters

- Breaks tenant isolation assumptions
- Can cause cross-tenant authorization leakage
- Makes incident investigation and data repair harder

## Example of Invalid but Currently Possible Data

- `users.id = 101, users.tenant_id = 1`
- `role.id = 205, role.tenant_id = 2`
- `user_role(user_id=101, role_id=205)` -> currently possible, but should be forbidden

## Recommended Future Fix

Implement in a new Flyway migration (for example `V2__enforce_user_role_tenant_consistency.sql`):

1. Add `tenant_id` to `user_role`
2. Backfill `tenant_id` from existing valid associations
3. Add composite constraints to enforce same-tenant links:
   - `(user_id, tenant_id)` references `users(id, tenant_id)`
   - `(role_id, tenant_id)` references `role(id, tenant_id)`
4. Add unique key `(tenant_id, user_id, role_id)`
5. Add data check/cleanup script before constraints if inconsistent rows exist

## Acceptance Criteria for Future Fix

- Inserting cross-tenant user-role relation fails at DB layer
- Existing valid data remains intact
- Migration is repeatable and rollback strategy is documented
- Integration test covers tenant-consistency constraint
