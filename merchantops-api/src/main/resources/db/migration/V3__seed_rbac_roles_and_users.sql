INSERT INTO `role` (tenant_id, role_code, role_name)
SELECT t.id, 'OPS_USER', 'Operations User'
FROM tenant t
WHERE t.tenant_code = 'demo-shop';

INSERT INTO `role` (tenant_id, role_code, role_name)
SELECT t.id, 'READ_ONLY', 'Read Only User'
FROM tenant t
WHERE t.tenant_code = 'demo-shop';

INSERT INTO users (tenant_id, username, password_hash, display_name, email, status)
SELECT
    t.id,
    'ops',
    u.password_hash,
    'Ops User',
    'ops@demo-shop.local',
    'ACTIVE'
FROM tenant t
         JOIN users u
              ON u.tenant_id = t.id
                  AND u.username = 'admin'
WHERE t.tenant_code = 'demo-shop';

INSERT INTO users (tenant_id, username, password_hash, display_name, email, status)
SELECT
    t.id,
    'viewer',
    u.password_hash,
    'Viewer User',
    'viewer@demo-shop.local',
    'ACTIVE'
FROM tenant t
         JOIN users u
              ON u.tenant_id = t.id
                  AND u.username = 'admin'
WHERE t.tenant_code = 'demo-shop';

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM users u
         JOIN tenant t
              ON u.tenant_id = t.id
         JOIN `role` r
              ON r.tenant_id = t.id
WHERE t.tenant_code = 'demo-shop'
  AND u.username = 'ops'
  AND r.role_code = 'OPS_USER';

INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM users u
         JOIN tenant t
              ON u.tenant_id = t.id
         JOIN `role` r
              ON r.tenant_id = t.id
WHERE t.tenant_code = 'demo-shop'
  AND u.username = 'viewer'
  AND r.role_code = 'READ_ONLY';

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM `role` r
         JOIN tenant t
              ON r.tenant_id = t.id
         JOIN permission p
WHERE t.tenant_code = 'demo-shop'
  AND r.role_code = 'OPS_USER'
  AND p.permission_code IN (
                            'USER_READ',
                            'ORDER_READ'
    );

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM `role` r
         JOIN tenant t
              ON r.tenant_id = t.id
         JOIN permission p
WHERE t.tenant_code = 'demo-shop'
  AND r.role_code = 'READ_ONLY'
  AND p.permission_code IN (
    'USER_READ'
    );
