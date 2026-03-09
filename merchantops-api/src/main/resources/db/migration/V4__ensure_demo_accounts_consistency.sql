INSERT INTO tenant (tenant_code, tenant_name, status)
SELECT 'demo-shop', 'Demo Shop', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1
    FROM tenant
    WHERE tenant_code = 'demo-shop'
);

UPDATE tenant
SET tenant_name = 'Demo Shop',
    status = 'ACTIVE'
WHERE tenant_code = 'demo-shop';

INSERT INTO permission (permission_code, permission_name)
SELECT src.permission_code, src.permission_name
FROM (
         SELECT 'USER_READ' AS permission_code, 'Read user' AS permission_name
         UNION ALL
         SELECT 'USER_WRITE', 'Write user'
         UNION ALL
         SELECT 'ORDER_READ', 'Read order'
         UNION ALL
         SELECT 'BILLING_READ', 'Read billing'
         UNION ALL
         SELECT 'FEATURE_FLAG_MANAGE', 'Manage feature flag'
     ) src
         LEFT JOIN permission p
                   ON p.permission_code = src.permission_code
WHERE p.id IS NULL;

INSERT INTO `role` (tenant_id, role_code, role_name)
SELECT t.id, src.role_code, src.role_name
FROM tenant t
         JOIN (
    SELECT 'TENANT_ADMIN' AS role_code, 'Tenant Admin' AS role_name
    UNION ALL
    SELECT 'OPS_USER', 'Operations User'
    UNION ALL
    SELECT 'READ_ONLY', 'Read Only User'
) src
         LEFT JOIN `role` r
                   ON r.tenant_id = t.id
                       AND r.role_code = src.role_code
WHERE t.tenant_code = 'demo-shop'
  AND r.id IS NULL;

INSERT INTO users (tenant_id, username, password_hash, display_name, email, status)
SELECT t.id, src.username, src.password_hash, src.display_name, src.email, 'ACTIVE'
FROM tenant t
         JOIN (
    SELECT 'admin' AS username,
           '$2a$10$C5/1udHsqaaa16O0xqkk0.RJp3vYeWe1ciwQhYT5bnJ2ogCU.QgY2' AS password_hash,
           'Demo Admin' AS display_name,
           'admin@demo-shop.local' AS email
    UNION ALL
    SELECT 'ops',
           '$2a$10$C5/1udHsqaaa16O0xqkk0.RJp3vYeWe1ciwQhYT5bnJ2ogCU.QgY2',
           'Ops User',
           'ops@demo-shop.local'
    UNION ALL
    SELECT 'viewer',
           '$2a$10$C5/1udHsqaaa16O0xqkk0.RJp3vYeWe1ciwQhYT5bnJ2ogCU.QgY2',
           'Viewer User',
           'viewer@demo-shop.local'
) src
         LEFT JOIN users u
                   ON u.tenant_id = t.id
                       AND u.username = src.username
WHERE t.tenant_code = 'demo-shop'
  AND u.id IS NULL;

UPDATE users u
    JOIN tenant t
         ON u.tenant_id = t.id
    JOIN (
    SELECT 'admin' AS username,
           '$2a$10$C5/1udHsqaaa16O0xqkk0.RJp3vYeWe1ciwQhYT5bnJ2ogCU.QgY2' AS password_hash,
           'Demo Admin' AS display_name,
           'admin@demo-shop.local' AS email
    UNION ALL
    SELECT 'ops',
           '$2a$10$C5/1udHsqaaa16O0xqkk0.RJp3vYeWe1ciwQhYT5bnJ2ogCU.QgY2',
           'Ops User',
           'ops@demo-shop.local'
    UNION ALL
    SELECT 'viewer',
           '$2a$10$C5/1udHsqaaa16O0xqkk0.RJp3vYeWe1ciwQhYT5bnJ2ogCU.QgY2',
           'Viewer User',
           'viewer@demo-shop.local'
) src
         ON src.username = u.username
SET u.password_hash = src.password_hash,
    u.display_name = src.display_name,
    u.email = src.email,
    u.status = 'ACTIVE'
WHERE t.tenant_code = 'demo-shop';

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM tenant t
         JOIN users u
              ON u.tenant_id = t.id
         JOIN (
    SELECT 'admin' AS username, 'TENANT_ADMIN' AS role_code
    UNION ALL
    SELECT 'ops', 'OPS_USER'
    UNION ALL
    SELECT 'viewer', 'READ_ONLY'
) src
              ON src.username = u.username
         JOIN `role` r
              ON r.tenant_id = t.id
                  AND r.role_code = src.role_code
         LEFT JOIN user_role ur
                   ON ur.user_id = u.id
                       AND ur.role_id = r.id
WHERE t.tenant_code = 'demo-shop'
  AND ur.id IS NULL;

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM tenant t
         JOIN `role` r
              ON r.tenant_id = t.id
         JOIN (
    SELECT 'TENANT_ADMIN' AS role_code, 'USER_READ' AS permission_code
    UNION ALL
    SELECT 'TENANT_ADMIN', 'USER_WRITE'
    UNION ALL
    SELECT 'TENANT_ADMIN', 'ORDER_READ'
    UNION ALL
    SELECT 'TENANT_ADMIN', 'BILLING_READ'
    UNION ALL
    SELECT 'TENANT_ADMIN', 'FEATURE_FLAG_MANAGE'
    UNION ALL
    SELECT 'OPS_USER', 'USER_READ'
    UNION ALL
    SELECT 'OPS_USER', 'ORDER_READ'
    UNION ALL
    SELECT 'READ_ONLY', 'USER_READ'
) src
              ON src.role_code = r.role_code
         JOIN permission p
              ON p.permission_code = src.permission_code
         LEFT JOIN role_permission rp
                   ON rp.role_id = r.id
                       AND rp.permission_id = p.id
WHERE t.tenant_code = 'demo-shop'
  AND rp.id IS NULL;
