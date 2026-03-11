ALTER TABLE users
    ADD CONSTRAINT uk_users_id_tenant UNIQUE (id, tenant_id);

CREATE TABLE audit_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id BIGINT NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    operator_id BIGINT NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    before_value TEXT NULL,
    after_value TEXT NULL,
    approval_status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_event_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_audit_event_operator_tenant FOREIGN KEY (operator_id, tenant_id) REFERENCES users(id, tenant_id)
);

CREATE INDEX idx_audit_event_entity ON audit_event (tenant_id, entity_type, entity_id, id);
CREATE INDEX idx_audit_event_created ON audit_event (tenant_id, created_at, id);
