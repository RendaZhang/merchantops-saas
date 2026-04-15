package com.renda.merchantops.api.support;

import org.springframework.jdbc.core.JdbcTemplate;

public final class TestAuthSessionSchemaSupport {

    private TestAuthSessionSchemaSupport() {
    }

    public static void createAuthSessionTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE auth_session (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    session_id VARCHAR(64) NOT NULL,
                    tenant_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    expires_at TIMESTAMP NOT NULL,
                    revoked_at TIMESTAMP
                )
                """);
        jdbcTemplate.execute("CREATE UNIQUE INDEX uk_auth_session_session_id ON auth_session (session_id)");
        jdbcTemplate.execute("CREATE INDEX idx_auth_session_tenant_user_status ON auth_session (tenant_id, user_id, status)");
        jdbcTemplate.execute("CREATE INDEX idx_auth_session_expires_at ON auth_session (expires_at)");
    }
}
