package com.renda.merchantops.api.integration;

import org.h2.Driver;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApprovalRequestMigrationTest {

    @Test
    void v12ShouldRejectLegacyDuplicatePendingDisableRowsAndKeepSinglePendingInvariant() throws Exception {
        String url = "jdbc:h2:mem:approval-request-migration-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
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
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V8__add_minimal_approval_request.sql"));
        }

        jdbcTemplate.update("INSERT INTO tenant (id) VALUES (?)", 1L);
        jdbcTemplate.update("INSERT INTO users (id, tenant_id) VALUES (?, ?)", 101L, 1L);

        insertApprovalRequest(
                jdbcTemplate,
                9001L,
                1L,
                "USER_STATUS_DISABLE",
                "USER",
                103L,
                101L,
                "PENDING",
                "disable-dup-old",
                "2026-03-20 10:00:00"
        );
        insertApprovalRequest(
                jdbcTemplate,
                9002L,
                1L,
                "USER_STATUS_DISABLE",
                "USER",
                103L,
                101L,
                "PENDING",
                "disable-dup-new",
                "2026-03-20 11:00:00"
        );
        insertApprovalRequest(
                jdbcTemplate,
                9003L,
                1L,
                "USER_STATUS_DISABLE",
                "USER",
                104L,
                101L,
                "PENDING",
                "disable-tie-low-id",
                "2026-03-20 12:00:00"
        );
        insertApprovalRequest(
                jdbcTemplate,
                9004L,
                1L,
                "USER_STATUS_DISABLE",
                "USER",
                104L,
                101L,
                "PENDING",
                "disable-tie-high-id",
                "2026-03-20 12:00:00"
        );
        insertApprovalRequest(
                jdbcTemplate,
                9005L,
                1L,
                "IMPORT_JOB_SELECTIVE_REPLAY",
                "IMPORT_JOB",
                7001L,
                101L,
                "PENDING",
                "import-replay-proposal",
                "2026-03-20 13:00:00"
        );
        insertApprovalRequest(
                jdbcTemplate,
                9006L,
                1L,
                "USER_STATUS_DISABLE",
                "USER",
                105L,
                101L,
                "REJECTED",
                "disable-rejected",
                "2026-03-20 14:00:00"
        );

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V12__enforce_pending_disable_uniqueness.sql"));
        }

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM approval_request WHERE id = ?",
                String.class,
                9001L
        )).isEqualTo("REJECTED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pending_request_key FROM approval_request WHERE id = ?",
                String.class,
                9001L
        )).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM approval_request WHERE id = ?",
                String.class,
                9002L
        )).isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pending_request_key FROM approval_request WHERE id = ?",
                String.class,
                9002L
        )).isEqualTo("USER_STATUS_DISABLE:1:103");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM approval_request WHERE id = ?",
                String.class,
                9003L
        )).isEqualTo("REJECTED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pending_request_key FROM approval_request WHERE id = ?",
                String.class,
                9003L
        )).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM approval_request WHERE id = ?",
                String.class,
                9004L
        )).isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pending_request_key FROM approval_request WHERE id = ?",
                String.class,
                9004L
        )).isEqualTo("USER_STATUS_DISABLE:1:104");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pending_request_key FROM approval_request WHERE id = ?",
                String.class,
                9005L
        )).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pending_request_key FROM approval_request WHERE id = ?",
                String.class,
                9006L
        )).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM approval_request WHERE pending_request_key IS NOT NULL",
                Integer.class
        )).isEqualTo(2);

        jdbcTemplate.update("""
                UPDATE approval_request
                SET status = ?, pending_request_key = NULL, reviewed_at = CURRENT_TIMESTAMP, executed_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, "APPROVED", 9002L);

        jdbcTemplate.update("""
                INSERT INTO approval_request (
                    id, tenant_id, action_type, entity_type, entity_id, requested_by, status, payload_json,
                    pending_request_key, request_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
                9009L, 1L, "USER_STATUS_DISABLE", "USER", 103L, 101L, "PENDING", "{\"status\":\"DISABLED\"}",
                "USER_STATUS_DISABLE:1:103", "disable-req-db-after-resolution");

        assertThat(jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM approval_request
                        WHERE tenant_id = ?
                          AND action_type = ?
                          AND entity_type = ?
                          AND entity_id = ?
                          AND status = ?
                        """,
                Integer.class,
                1L,
                "USER_STATUS_DISABLE",
                "USER",
                103L,
                "PENDING"
        )).isEqualTo(1);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                        INSERT INTO approval_request (
                            id, tenant_id, action_type, entity_type, entity_id, requested_by, status, payload_json,
                            pending_request_key, request_id, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                        """,
                9010L, 1L, "USER_STATUS_DISABLE", "USER", 103L, 101L, "PENDING", "{\"status\":\"DISABLED\"}",
                "USER_STATUS_DISABLE:1:103", "disable-req-db-after-v12"))
                .isInstanceOf(DataAccessException.class);
    }

    private void insertApprovalRequest(JdbcTemplate jdbcTemplate,
                                       Long id,
                                       Long tenantId,
                                       String actionType,
                                       String entityType,
                                       Long entityId,
                                       Long requestedBy,
                                       String status,
                                       String requestId,
                                       String createdAt) {
        jdbcTemplate.update("""
                INSERT INTO approval_request (
                    id, tenant_id, action_type, entity_type, entity_id, requested_by, status, payload_json, request_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, PARSEDATETIME(?, 'yyyy-MM-dd HH:mm:ss'))
                """, id, tenantId, actionType, entityType, entityId, requestedBy, status, "{\"status\":\"DISABLED\"}", requestId, createdAt);
    }
}
