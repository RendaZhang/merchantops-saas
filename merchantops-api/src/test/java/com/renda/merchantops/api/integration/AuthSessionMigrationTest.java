package com.renda.merchantops.api.integration;

import org.h2.Driver;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthSessionMigrationTest {

    @Test
    void v18ShouldApplyAfterV15PreserveExistingRowsAndKeepAuthSessionTimesWritable() throws Exception {
        String url = "jdbc:h2:mem:auth-session-migration-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource(new Driver(), url, "sa", "");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("""
                CREATE TABLE tenant (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE users (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    tenant_id BIGINT NOT NULL,
                    CONSTRAINT uk_users_id_tenant UNIQUE (id, tenant_id),
                    CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
                )
                """);

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V15__add_auth_session.sql"));
        }

        jdbcTemplate.update("INSERT INTO tenant (id) VALUES (?)", 1L);
        jdbcTemplate.update("INSERT INTO users (id, tenant_id) VALUES (?, ?)", 101L, 1L);
        jdbcTemplate.update("""
                INSERT INTO auth_session (session_id, tenant_id, user_id, status, created_at, expires_at, revoked_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                "legacy-session",
                1L,
                101L,
                "ACTIVE",
                Timestamp.valueOf("2026-04-25 10:00:00"),
                Timestamp.valueOf("2026-04-25 12:00:00"),
                null
        );

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V18__store_auth_session_times_as_datetime.sql"));
        }

        assertAuthSessionTimes(jdbcTemplate, "legacy-session", "2026-04-25 10:00:00", "2026-04-25 12:00:00");

        jdbcTemplate.update("""
                INSERT INTO auth_session (session_id, tenant_id, user_id, status, created_at, expires_at, revoked_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                "post-v18-session",
                1L,
                101L,
                "ACTIVE",
                Timestamp.valueOf("2026-04-25 13:00:00"),
                Timestamp.valueOf("2026-04-25 15:00:00"),
                null
        );

        assertAuthSessionTimes(jdbcTemplate, "post-v18-session", "2026-04-25 13:00:00", "2026-04-25 15:00:00");
    }

    private void assertAuthSessionTimes(JdbcTemplate jdbcTemplate,
                                        String sessionId,
                                        String expectedCreatedAt,
                                        String expectedExpiresAt) {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT created_at FROM auth_session WHERE session_id = ?",
                Timestamp.class,
                sessionId
        )).isEqualTo(Timestamp.valueOf(expectedCreatedAt));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT expires_at FROM auth_session WHERE session_id = ?",
                Timestamp.class,
                sessionId
        )).isEqualTo(Timestamp.valueOf(expectedExpiresAt));
    }
}
