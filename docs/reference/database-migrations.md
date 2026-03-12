# Database Migrations

## Flyway Setup

- Flyway dependencies are managed in `merchantops-infra`
- Migration location: `merchantops-api/src/main/resources/db/migration`
- Naming pattern: `V{version}__{description}.sql`
- In the `dev` profile, Flyway runs automatically on startup

## Current Migrations

- `V1__init_schema.sql`: creates base tenant and RBAC tables
- `V2__seed_demo_data.sql`: inserts the first demo tenant, admin user, roles, and permissions
- `V3__seed_rbac_roles_and_users.sql`: adds demo RBAC roles and the `ops` / `viewer` users
- `V4__ensure_demo_accounts_consistency.sql`: idempotently enforces demo tenant/users/roles/permissions consistency
- `V5__add_user_operator_tracking.sql`: adds nullable `created_by` and `updated_by` columns to `users` for lightweight operator attribution
- `V6__add_ticket_workflow.sql`: adds `ticket`, `ticket_comment`, and `ticket_operation_log`, plus `TICKET_READ` / `TICKET_WRITE` permission seed data
- `V7__add_audit_event_backbone.sql`: adds tenant-scoped `audit_event` for reusable governance audit snapshots across existing public write flows, with DB-level same-tenant linkage between `audit_event.operator_id` and `audit_event.tenant_id`
- `V8__add_minimal_approval_request.sql`: adds tenant-scoped `approval_request` for the first minimal approval flow (`USER_STATUS_DISABLE`), including same-tenant requester/reviewer foreign-key linkage

## Demo Accounts

Tenant: `demo-shop`

- `admin` / `123456`
- `ops` / `123456`
- `viewer` / `123456`

To verify the accounts really exist in database:

```sql
SELECT t.tenant_code, u.username, u.display_name, u.status
FROM users u
JOIN tenant t ON t.id = u.tenant_id
WHERE t.tenant_code = 'demo-shop'
  AND u.username IN ('admin', 'ops', 'viewer')
ORDER BY u.username;
```

To verify lightweight operator attribution after Week 2 write operations:

```sql
SELECT id, username, created_by, updated_by
FROM users
WHERE tenant_id = 1
ORDER BY id;
```

To verify ticket tables and demo ticket permissions after the completed Week 3 ticket workflow baseline:

```sql
SELECT permission_code
FROM permission
WHERE permission_code IN ('TICKET_READ', 'TICKET_WRITE')
ORDER BY permission_code;

SELECT r.role_code, p.permission_code
FROM role_permission rp
JOIN `role` r ON r.id = rp.role_id
JOIN permission p ON p.id = rp.permission_id
JOIN tenant t ON t.id = r.tenant_id
WHERE t.tenant_code = 'demo-shop'
  AND p.permission_code IN ('TICKET_READ', 'TICKET_WRITE')
ORDER BY r.role_code, p.permission_code;

SELECT COUNT(*) AS ticket_cnt FROM ticket;
SELECT COUNT(*) AS ticket_comment_cnt FROM ticket_comment;
SELECT COUNT(*) AS ticket_operation_log_cnt FROM ticket_operation_log;
SELECT COUNT(*) AS audit_event_cnt FROM audit_event;
SELECT COUNT(*) AS approval_request_cnt FROM approval_request;
```

## Verify Migration History

```sql
SELECT installed_rank, version, description, script, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

## Verify Seeded Data

```sql
SELECT
  (SELECT COUNT(*) FROM tenant) AS tenant_cnt,
  (SELECT COUNT(*) FROM role) AS role_cnt,
  (SELECT COUNT(*) FROM permission) AS perm_cnt,
  (SELECT COUNT(*) FROM users) AS user_cnt,
  (SELECT COUNT(*) FROM user_role) AS user_role_cnt,
  (SELECT COUNT(*) FROM role_permission) AS role_perm_cnt,
  (SELECT COUNT(*) FROM ticket) AS ticket_cnt,
  (SELECT COUNT(*) FROM ticket_comment) AS ticket_comment_cnt,
  (SELECT COUNT(*) FROM ticket_operation_log) AS ticket_operation_log_cnt;
```

## Related Notes

- Password hashes for seed users can be generated with `merchantops-api/src/main/java/com/renda/merchantops/api/tools/PasswordHashGenerator.java`
- Do not edit an already-applied migration. Create a new version for follow-up changes instead.
- `created_by` and `updated_by` are intentionally nullable so historical seed rows do not pretend to have a synthetic operator.
- Known schema gap: [../architecture/non-blocking-backlog.md#nb-001-user-role-database-level-tenant-integrity](../architecture/non-blocking-backlog.md#nb-001-user-role-database-level-tenant-integrity)
