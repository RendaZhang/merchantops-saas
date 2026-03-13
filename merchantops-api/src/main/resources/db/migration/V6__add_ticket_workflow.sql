CREATE TABLE ticket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    assignee_id BIGINT NULL,
    created_by BIGINT NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_ticket_assignee FOREIGN KEY (assignee_id) REFERENCES users(id),
    CONSTRAINT fk_ticket_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_ticket_tenant_updated ON ticket (tenant_id, updated_at, id);
CREATE INDEX idx_ticket_tenant_status_updated ON ticket (tenant_id, status, updated_at, id);

CREATE TABLE ticket_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    ticket_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_by BIGINT NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_comment_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_ticket_comment_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(id),
    CONSTRAINT fk_ticket_comment_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_ticket_comment_ticket ON ticket_comment (ticket_id, tenant_id, id);

CREATE TABLE ticket_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    ticket_id BIGINT NOT NULL,
    operation_type VARCHAR(64) NOT NULL,
    detail VARCHAR(512) NOT NULL,
    operator_id BIGINT NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_operation_log_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_ticket_operation_log_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(id),
    CONSTRAINT fk_ticket_operation_log_operator FOREIGN KEY (operator_id) REFERENCES users(id)
);

CREATE INDEX idx_ticket_operation_log_ticket ON ticket_operation_log (ticket_id, tenant_id, id);

INSERT INTO permission (permission_code, permission_name)
SELECT src.permission_code, src.permission_name
FROM (
    SELECT 'TICKET_READ' AS permission_code, 'Read ticket' AS permission_name
    UNION ALL
    SELECT 'TICKET_WRITE', 'Write ticket'
) src
LEFT JOIN permission p
       ON p.permission_code = src.permission_code
WHERE p.id IS NULL;

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM tenant t
JOIN `role` r
  ON r.tenant_id = t.id
JOIN (
    SELECT 'TENANT_ADMIN' AS role_code, 'TICKET_READ' AS permission_code
    UNION ALL
    SELECT 'TENANT_ADMIN', 'TICKET_WRITE'
    UNION ALL
    SELECT 'OPS_USER', 'TICKET_READ'
    UNION ALL
    SELECT 'OPS_USER', 'TICKET_WRITE'
    UNION ALL
    SELECT 'READ_ONLY', 'TICKET_READ'
) src
  ON src.role_code = r.role_code
JOIN permission p
  ON p.permission_code = src.permission_code
LEFT JOIN role_permission rp
       ON rp.role_id = r.id
      AND rp.permission_id = p.id
WHERE t.tenant_code = 'demo-shop'
  AND rp.id IS NULL;
