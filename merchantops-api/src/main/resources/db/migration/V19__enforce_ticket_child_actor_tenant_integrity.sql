CREATE INDEX idx_ticket_comment_created_by_tenant
    ON ticket_comment (created_by, tenant_id);

CREATE INDEX idx_ticket_operation_log_operator_tenant
    ON ticket_operation_log (operator_id, tenant_id);

ALTER TABLE ticket_comment
    ADD CONSTRAINT fk_ticket_comment_created_by_tenant
        FOREIGN KEY (created_by, tenant_id) REFERENCES users(id, tenant_id);

ALTER TABLE ticket_operation_log
    ADD CONSTRAINT fk_ticket_operation_log_operator_tenant
        FOREIGN KEY (operator_id, tenant_id) REFERENCES users(id, tenant_id);
