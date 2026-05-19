CREATE TABLE auth_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    CONSTRAINT fk_auth_session_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_auth_session_user_tenant
        FOREIGN KEY (user_id, tenant_id) REFERENCES users(id, tenant_id)
);

CREATE UNIQUE INDEX uk_auth_session_session_id
    ON auth_session (session_id);

CREATE INDEX idx_auth_session_tenant_user_status
    ON auth_session (tenant_id, user_id, status);

CREATE INDEX idx_auth_session_expires_at
    ON auth_session (expires_at);
