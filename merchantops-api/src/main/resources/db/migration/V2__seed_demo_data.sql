INSERT INTO tenant (tenant_code, tenant_name, status)
VALUES ('demo-shop', 'Demo Shop', 'ACTIVE');

INSERT INTO `role` (tenant_id, role_code, role_name)
SELECT id, 'TENANT_ADMIN', 'Tenant Admin'
FROM tenant
WHERE tenant_code = 'demo-shop';

INSERT INTO permission (permission_code, permission_name)
VALUES
    ('USER_READ', 'Read user'),
    ('USER_WRITE', 'Write user'),
    ('ORDER_READ', 'Read order'),
    ('BILLING_READ', 'Read billing'),
    ('FEATURE_FLAG_MANAGE', 'Manage feature flag');

INSERT INTO users (tenant_id, username, password_hash, display_name, email, status)
SELECT
    id,
    'admin',
    '$2a$10$C5/1udHsqaaa16O0xqkk0.RJp3vYeWe1ciwQhYT5bnJ2ogCU.QgY2',
    'Demo Admin',
    'admin@demo-shop.local',
    'ACTIVE'
FROM tenant
WHERE tenant_code = 'demo-shop';

INSERT INTO user_role (user_id, role_id)
SELECT
    u.id,
    r.id
FROM users u
         JOIN tenant t ON u.tenant_id = t.id
         JOIN `role` r ON r.tenant_id = t.id
WHERE t.tenant_code = 'demo-shop'
  AND u.username = 'admin'
  AND r.role_code = 'TENANT_ADMIN';

INSERT INTO role_permission (role_id, permission_id)
SELECT
    r.id,
    p.id
FROM `role` r
         JOIN tenant t ON r.tenant_id = t.id
         JOIN permission p
WHERE t.tenant_code = 'demo-shop'
  AND r.role_code = 'TENANT_ADMIN'
  AND p.permission_code IN (
                            'USER_READ',
                            'USER_WRITE',
                            'ORDER_READ',
                            'BILLING_READ',
                            'FEATURE_FLAG_MANAGE'
    );
