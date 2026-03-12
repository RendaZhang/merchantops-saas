CREATE TABLE approval_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id BIGINT NOT NULL,
    requested_by BIGINT NOT NULL,
    reviewed_by BIGINT NULL,
    status VARCHAR(32) NOT NULL,
    payload_json TEXT NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME NULL,
    executed_at DATETIME NULL,
    CONSTRAINT fk_approval_request_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_approval_request_requested_by_tenant FOREIGN KEY (requested_by, tenant_id) REFERENCES users(id, tenant_id),
    CONSTRAINT fk_approval_request_reviewed_by_tenant FOREIGN KEY (reviewed_by, tenant_id) REFERENCES users(id, tenant_id)
);

CREATE INDEX idx_approval_request_lookup ON approval_request (tenant_id, id);
CREATE INDEX idx_approval_request_entity ON approval_request (tenant_id, entity_type, entity_id, id);
