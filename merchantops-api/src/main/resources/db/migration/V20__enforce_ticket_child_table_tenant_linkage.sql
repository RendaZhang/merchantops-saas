ALTER TABLE ticket
    ADD CONSTRAINT uk_ticket_id_tenant UNIQUE (id, tenant_id);

ALTER TABLE ticket_comment
    ADD CONSTRAINT fk_ticket_comment_ticket_tenant
        FOREIGN KEY (ticket_id, tenant_id) REFERENCES ticket(id, tenant_id);

ALTER TABLE ticket_operation_log
    ADD CONSTRAINT fk_ticket_operation_log_ticket_tenant
        FOREIGN KEY (ticket_id, tenant_id) REFERENCES ticket(id, tenant_id);
