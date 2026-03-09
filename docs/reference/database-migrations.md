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

## Demo Accounts

Tenant: `demo-shop`

- `admin` / `123456`
- `ops` / `123456`
- `viewer` / `123456`

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
  (SELECT COUNT(*) FROM role_permission) AS role_perm_cnt;
```

## Related Notes

- Password hashes for seed users can be generated with `merchantops-api/src/main/java/com/renda/merchantops/api/tools/PasswordHashGenerator.java`
- Do not edit an already-applied migration. Create a new version for follow-up changes instead.
- Known schema gap: [../architecture/tenant-rbac-integrity-gap.md](../architecture/tenant-rbac-integrity-gap.md)
