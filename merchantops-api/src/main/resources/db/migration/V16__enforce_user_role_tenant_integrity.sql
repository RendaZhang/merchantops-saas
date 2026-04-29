ALTER TABLE user_role
    ADD COLUMN tenant_id BIGINT NULL;

UPDATE user_role ur
    JOIN users u ON u.id = ur.user_id
SET ur.tenant_id = u.tenant_id;

ALTER TABLE user_role
    MODIFY tenant_id BIGINT NOT NULL;

ALTER TABLE `role`
    ADD CONSTRAINT uk_role_id_tenant UNIQUE (id, tenant_id);

CREATE INDEX idx_user_role_user_tenant
    ON user_role (user_id, tenant_id);

CREATE INDEX idx_user_role_role_tenant
    ON user_role (role_id, tenant_id);

ALTER TABLE user_role
    ADD CONSTRAINT fk_user_role_user_tenant
        FOREIGN KEY (user_id, tenant_id) REFERENCES users(id, tenant_id);

ALTER TABLE user_role
    ADD CONSTRAINT fk_user_role_role_tenant
        FOREIGN KEY (role_id, tenant_id) REFERENCES `role`(id, tenant_id);
