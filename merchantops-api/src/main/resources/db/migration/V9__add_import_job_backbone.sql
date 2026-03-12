CREATE TABLE import_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    import_type VARCHAR(64) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    requested_by BIGINT NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    total_count INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    failure_count INT NOT NULL DEFAULT 0,
    error_summary VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    CONSTRAINT uk_import_job_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_import_job_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_import_job_requested_by_tenant FOREIGN KEY (requested_by, tenant_id) REFERENCES users(id, tenant_id)
);

CREATE TABLE import_job_item_error (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    import_job_id BIGINT NOT NULL,
    source_row_number INT NULL,
    error_code VARCHAR(64) NOT NULL,
    error_message VARCHAR(512) NOT NULL,
    raw_payload TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_import_job_item_error_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_import_job_item_error_job_tenant FOREIGN KEY (tenant_id, import_job_id) REFERENCES import_job(tenant_id, id)
);

CREATE INDEX idx_import_job_page ON import_job (tenant_id, created_at DESC, id DESC);
CREATE INDEX idx_import_job_item_error_lookup ON import_job_item_error (tenant_id, import_job_id, id);
