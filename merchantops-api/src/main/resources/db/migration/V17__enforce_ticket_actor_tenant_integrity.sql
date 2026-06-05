CREATE INDEX idx_ticket_assignee_tenant
    ON ticket (assignee_id, tenant_id);

CREATE INDEX idx_ticket_created_by_tenant
    ON ticket (created_by, tenant_id);

ALTER TABLE ticket
    ADD CONSTRAINT fk_ticket_assignee_tenant
        FOREIGN KEY (assignee_id, tenant_id) REFERENCES users(id, tenant_id);

ALTER TABLE ticket
    ADD CONSTRAINT fk_ticket_created_by_tenant
        FOREIGN KEY (created_by, tenant_id) REFERENCES users(id, tenant_id);
