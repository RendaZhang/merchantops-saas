# Database Migrations

## Flyway Setup

- Flyway dependencies are managed in `merchantops-infra`
- SQL migration location: `merchantops-api/src/main/resources/db/migration`
- Java migration location: `merchantops-api/src/main/java/db/migration`
- Naming pattern:
  - SQL: `V{version}__{description}.sql`
  - Java: `V{version}__{description}.java`
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
- `V9__add_import_job_backbone.sql`: adds tenant-scoped `import_job` and `import_job_item_error` for Week 5 async import submission, queue processing, and row-level error tracking, including DB-level same-tenant linkage from item errors back to the parent import job
- `V10__add_import_job_replay_lineage.sql`: adds nullable `import_job.source_job_id` so replay-derived jobs can point back to the source job, including DB-level same-tenant lineage protection through `(tenant_id, source_job_id) -> import_job(tenant_id, id)`
- `V11__add_ai_interaction_record.sql`: adds tenant-scoped `ai_interaction_record` for Week 6 AI runtime traceability, including same-tenant linkage between `tenant_id` and `user_id` plus indexed lookup by entity and request id
- `V12__enforce_pending_disable_uniqueness.sql`: adds nullable `approval_request.pending_request_key`, converts superseded historical duplicate pending `USER_STATUS_DISABLE` rows for the same tenant user to `REJECTED`, backfills only the canonical newest pending row with the derived key, and creates a unique index so future pending disable requests stay unique per tenant user at the database layer
- `V13__harden_pending_proposal_uniqueness.java`: extends pending-request-key governance across `IMPORT_JOB_SELECTIVE_REPLAY` and `TICKET_COMMENT_CREATE`, backfills canonical pending keys for those actions, clears stale keys from resolved rows, and converts superseded duplicate pending proposal rows to `REJECTED` before the shared unique index continues enforcing one pending executable intent per key
- `V14__add_feature_flags.sql`: adds tenant-scoped fixed-key `feature_flag` rows for AI generation and workflow-bridge rollout control
- `V15__add_auth_session.sql`: adds `auth_session` for server-side access-token session state, including unique `session_id`, tenant/user linkage, `ACTIVE` / `REVOKED` status, expiry, revocation timestamp, and lookup indexes
- `V16__enforce_user_role_tenant_integrity.sql`: adds `user_role.tenant_id`, backfills it from `users.tenant_id`, adds `role(id, tenant_id)` uniqueness plus child indexes, and enforces `user_role(user_id, tenant_id) -> users(id, tenant_id)` plus `user_role(role_id, tenant_id) -> role(id, tenant_id)` so role bindings must stay within one tenant
- `V17__enforce_ticket_actor_tenant_integrity.sql`: adds child indexes and enforces `ticket(assignee_id, tenant_id) -> users(id, tenant_id)` plus `ticket(created_by, tenant_id) -> users(id, tenant_id)` so root ticket assignee and creator references must stay within the ticket tenant; `assignee_id` remains nullable for unassigned tickets

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

## Useful Verification Queries

To verify lightweight operator attribution after Week 2 write operations:

```sql
SELECT id, username, created_by, updated_by
FROM users
WHERE tenant_id = 1
ORDER BY id;
```

To verify ticket tables, import tables, and the Week 6 AI interaction table exist:

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
SELECT COUNT(*) AS import_job_cnt FROM import_job;
SELECT COUNT(*) AS import_job_item_error_cnt FROM import_job_item_error;
SELECT COUNT(*) AS ai_interaction_record_cnt FROM ai_interaction_record;
SELECT COUNT(*) AS feature_flag_cnt FROM feature_flag;
SELECT COUNT(*) AS auth_session_cnt FROM auth_session;
```

To verify the `user_role` tenant-integrity invariant after `V16`:

```sql
SELECT ur.id,
       ur.tenant_id,
       u.tenant_id AS user_tenant_id,
       r.tenant_id AS role_tenant_id
FROM user_role ur
JOIN users u ON u.id = ur.user_id
JOIN `role` r ON r.id = ur.role_id
WHERE ur.tenant_id <> u.tenant_id
   OR ur.tenant_id <> r.tenant_id;
```

The query should return no rows. Direct inserts that try to bind a user from one tenant to a role from another tenant should fail at the database layer.

To verify the root ticket actor tenant-integrity invariant after `V17`:

```sql
SELECT tk.id,
       tk.tenant_id,
       tk.assignee_id,
       assignee.tenant_id AS assignee_tenant_id,
       tk.created_by,
       creator.tenant_id AS creator_tenant_id
FROM ticket tk
LEFT JOIN users assignee ON assignee.id = tk.assignee_id
LEFT JOIN users creator ON creator.id = tk.created_by
WHERE (tk.assignee_id IS NOT NULL AND (assignee.id IS NULL OR assignee.tenant_id <> tk.tenant_id))
   OR creator.id IS NULL
   OR creator.tenant_id <> tk.tenant_id;
```

The query should return no rows. Direct inserts that try to create a ticket with a cross-tenant assignee or creator should fail at the database layer, while `assignee_id = NULL` remains valid for unassigned tickets.

To inspect recent AI interaction rows for ticket summary or ticket triage:

```sql
SELECT id,
       tenant_id,
       user_id,
       request_id,
       entity_type,
       entity_id,
       interaction_type,
       prompt_version,
       model_id,
       status,
       latency_ms,
       created_at
FROM ai_interaction_record
ORDER BY id DESC;
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
  (SELECT COUNT(*) FROM ticket_operation_log) AS ticket_operation_log_cnt,
  (SELECT COUNT(*) FROM ai_interaction_record) AS ai_interaction_record_cnt,
  (SELECT COUNT(*) FROM feature_flag) AS feature_flag_cnt,
  (SELECT COUNT(*) FROM auth_session) AS auth_session_cnt;
```

## Related Notes

- Password hashes for seed users can be generated with `merchantops-api/src/main/java/com/renda/merchantops/api/tools/PasswordHashGenerator.java`
- Do not edit an already-applied migration once it is part of a tagged or otherwise shared baseline. The current `V12__enforce_pending_disable_uniqueness.sql` rewrite is a narrow pre-tag exception because a fresh MySQL install failed before later migrations could run.
- If a local development database already applied the pre-fix `V12`, prefer dropping and recreating that schema so Flyway can replay the corrected migration chain cleanly. If the schema must be kept, confirm the rewritten `V12` is semantically equivalent first, then repair the `flyway_schema_history` checksum with an external Flyway client before the next startup.
- `created_by` and `updated_by` are intentionally nullable so historical seed rows do not pretend to have a synthetic operator.
- `ai_interaction_record` is intentionally separate from `audit_event`; see [../architecture/adr/0012-keep-ai-interaction-records-separate-from-generic-audit-events.md](../architecture/adr/0012-keep-ai-interaction-records-separate-from-generic-audit-events.md).
- `user_role` now carries its own `tenant_id` and is protected by same-tenant composite foreign keys.
- Root ticket assignee and creator references are protected by same-tenant composite foreign keys from `ticket` to `users`.
- Remaining ticket actor-link schema gaps are tracked separately in [../architecture/non-blocking-backlog.md#nb-002-ticket-actor-tenant-integrity-at-the-database-layer](../architecture/non-blocking-backlog.md#nb-002-ticket-actor-tenant-integrity-at-the-database-layer).
