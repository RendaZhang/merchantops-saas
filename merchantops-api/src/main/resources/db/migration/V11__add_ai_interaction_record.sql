CREATE TABLE ai_interaction_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id BIGINT NOT NULL,
    interaction_type VARCHAR(64) NOT NULL,
    prompt_version VARCHAR(128) NOT NULL,
    model_id VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL,
    latency_ms BIGINT NOT NULL,
    output_summary TEXT NULL,
    usage_prompt_tokens INT NULL,
    usage_completion_tokens INT NULL,
    usage_total_tokens INT NULL,
    usage_cost_micros BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_interaction_record_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_ai_interaction_record_user_tenant FOREIGN KEY (user_id, tenant_id) REFERENCES users(id, tenant_id)
);

CREATE INDEX idx_ai_interaction_record_entity ON ai_interaction_record (tenant_id, entity_type, entity_id, id);
CREATE INDEX idx_ai_interaction_record_request ON ai_interaction_record (tenant_id, request_id, id);
