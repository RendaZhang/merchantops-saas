ALTER TABLE import_job
    ADD COLUMN source_job_id BIGINT NULL;

ALTER TABLE import_job
    ADD CONSTRAINT fk_import_job_source_job_tenant
        FOREIGN KEY (tenant_id, source_job_id) REFERENCES import_job(tenant_id, id);

CREATE INDEX idx_import_job_source_lookup ON import_job (tenant_id, source_job_id, created_at DESC, id DESC);
