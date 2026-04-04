package com.renda.merchantops.api.integration;

import db.migration.V13__harden_pending_proposal_uniqueness;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.migration.Context;
import org.h2.Driver;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.HexFormat;
import java.util.List;
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
                "SELECT reviewed_at FROM approval_request WHERE id = ?",
                Timestamp.class,
                9001L
        )).isEqualTo(Timestamp.valueOf("2026-03-20 10:00:00"));
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
                "SELECT reviewed_at FROM approval_request WHERE id = ?",
                Timestamp.class,
                9003L
        )).isEqualTo(Timestamp.valueOf("2026-03-20 12:00:00"));
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

    @Test
    void v13ShouldBackfillPendingKeysAndCollapseLegacyDuplicateProposalRows() throws Exception {
        String url = "jdbc:h2:mem:approval-request-migration-v13-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
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
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V12__enforce_pending_disable_uniqueness.sql"));
        }

        jdbcTemplate.update("INSERT INTO tenant (id) VALUES (?)", 1L);
        jdbcTemplate.update("INSERT INTO users (id, tenant_id) VALUES (?, ?)", 101L, 1L);
        jdbcTemplate.update("INSERT INTO users (id, tenant_id) VALUES (?, ?)", 105L, 1L);

        insertApprovalRequest(
                jdbcTemplate,
                9101L,
                1L,
                "IMPORT_JOB_SELECTIVE_REPLAY",
                "IMPORT_JOB",
                7001L,
                101L,
                "PENDING",
                "{\"sourceJobId\":7001,\"errorCodes\":[\"UNKNOWN_ROLE\",\"INVALID_EMAIL\",\"UNKNOWN_ROLE\"],\"sourceInteractionId\":9103,\"proposalReason\":\"first\"}",
                null,
                "import-proposal-old",
                "2026-03-30 10:00:00",
                null,
                null,
                null
        );
        insertApprovalRequest(
                jdbcTemplate,
                9102L,
                1L,
                "IMPORT_JOB_SELECTIVE_REPLAY",
                "IMPORT_JOB",
                7001L,
                101L,
                "PENDING",
                "{\"sourceJobId\":7001,\"errorCodes\":[\"INVALID_EMAIL\",\"UNKNOWN_ROLE\"],\"sourceInteractionId\":9104,\"proposalReason\":\"second\"}",
                null,
                "import-proposal-new",
                "2026-03-30 11:00:00",
                null,
                null,
                null
        );
        insertApprovalRequest(
                jdbcTemplate,
                9103L,
                1L,
                "IMPORT_JOB_SELECTIVE_REPLAY",
                "IMPORT_JOB",
                7002L,
                101L,
                "PENDING",
                "{\"sourceJobId\":7002,\"errorCodes\":[\"UNKNOWN_ROLE\"]}",
                null,
                "import-proposal-unique",
                "2026-03-30 12:00:00",
                null,
                null,
                null
        );
        insertApprovalRequest(
                jdbcTemplate,
                9201L,
                1L,
                "TICKET_COMMENT_CREATE",
                "TICKET",
                301L,
                101L,
                "PENDING",
                "{\"commentContent\":\"  Hello store team  \",\"sourceInteractionId\":9002}",
                null,
                "ticket-proposal-old",
                "2026-03-30 10:30:00",
                null,
                null,
                null
        );
        insertApprovalRequest(
                jdbcTemplate,
                9202L,
                1L,
                "TICKET_COMMENT_CREATE",
                "TICKET",
                301L,
                101L,
                "PENDING",
                "{\"commentContent\":\"Hello store team\"}",
                null,
                "ticket-proposal-new",
                "2026-03-30 11:30:00",
                null,
                null,
                null
        );
        insertApprovalRequest(
                jdbcTemplate,
                9203L,
                1L,
                "TICKET_COMMENT_CREATE",
                "TICKET",
                302L,
                101L,
                "PENDING",
                "{\"commentContent\":\"Different content\"}",
                null,
                "ticket-proposal-unique",
                "2026-03-30 12:30:00",
                null,
                null,
                null
        );
        insertApprovalRequest(
                jdbcTemplate,
                9301L,
                1L,
                "USER_STATUS_DISABLE",
                "USER",
                103L,
                101L,
                "PENDING",
                "{\"status\":\"DISABLED\"}",
                userStatusDisableKey(1L, 103L),
                "disable-pending",
                "2026-03-30 13:00:00",
                null,
                null,
                null
        );
        insertApprovalRequest(
                jdbcTemplate,
                9401L,
                1L,
                "IMPORT_JOB_SELECTIVE_REPLAY",
                "IMPORT_JOB",
                7003L,
                101L,
                "APPROVED",
                "{\"sourceJobId\":7003,\"errorCodes\":[\"UNKNOWN_ROLE\"]}",
                "stale-import-key",
                "import-approved",
                "2026-03-30 09:00:00",
                105L,
                "2026-03-30 09:05:00",
                "2026-03-30 09:05:00"
        );
        insertApprovalRequest(
                jdbcTemplate,
                9402L,
                1L,
                "TICKET_COMMENT_CREATE",
                "TICKET",
                303L,
                101L,
                "REJECTED",
                "{\"commentContent\":\"Already rejected\"}",
                "stale-ticket-key",
                "ticket-rejected",
                "2026-03-30 09:30:00",
                105L,
                "2026-03-30 09:35:00",
                null
        );

        try (Connection connection = dataSource.getConnection()) {
            new V13__harden_pending_proposal_uniqueness().migrate(context(connection));
        }

        assertThat(jdbcTemplate.queryForObject(
                "SELECT pending_request_key FROM approval_request WHERE id = ?",
                String.class,
                9301L
        )).isEqualTo(userStatusDisableKey(1L, 103L));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM approval_request WHERE id = ?",
                String.class,
                9101L
        )).isEqualTo("REJECTED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pending_request_key FROM approval_request WHERE id = ?",
                String.class,
                9101L
        )).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT reviewed_by FROM approval_request WHERE id = ?",
                Long.class,
                9101L
        )).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT reviewed_at FROM approval_request WHERE id = ?",
                Timestamp.class,
                9101L
        )).isEqualTo(Timestamp.valueOf("2026-03-30 10:00:00"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT executed_at FROM approval_request WHERE id = ?",
                Timestamp.class,
                9101L
        )).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pending_request_key FROM approval_request WHERE id = ?",
                String.class,
                9102L
        )).isEqualTo(importJobSelectiveReplayKey(1L, 7001L, List.of("INVALID_EMAIL", "UNKNOWN_ROLE")));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pending_request_key FROM approval_request WHERE id = ?",
                String.class,
                9103L
        )).isEqualTo(importJobSelectiveReplayKey(1L, 7002L, List.of("UNKNOWN_ROLE")));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM approval_request WHERE id = ?",
                String.class,
                9201L
        )).isEqualTo("REJECTED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pending_request_key FROM approval_request WHERE id = ?",
                String.class,
                9201L
        )).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pending_request_key FROM approval_request WHERE id = ?",
                String.class,
                9202L
        )).isEqualTo(ticketCommentCreateKey(1L, 301L, "Hello store team"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pending_request_key FROM approval_request WHERE id = ?",
                String.class,
                9203L
        )).isEqualTo(ticketCommentCreateKey(1L, 302L, "Different content"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT pending_request_key FROM approval_request WHERE id = ?",
                String.class,
                9401L
        )).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pending_request_key FROM approval_request WHERE id = ?",
                String.class,
                9402L
        )).isNull();
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

    private void insertApprovalRequest(JdbcTemplate jdbcTemplate,
                                       Long id,
                                       Long tenantId,
                                       String actionType,
                                       String entityType,
                                       Long entityId,
                                       Long requestedBy,
                                       String status,
                                       String payloadJson,
                                       String pendingRequestKey,
                                       String requestId,
                                       String createdAt,
                                       Long reviewedBy,
                                       String reviewedAt,
                                       String executedAt) {
        jdbcTemplate.update("""
                INSERT INTO approval_request (
                    id, tenant_id, action_type, entity_type, entity_id, requested_by, reviewed_by, status,
                    payload_json, pending_request_key, request_id, created_at, reviewed_at, executed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, PARSEDATETIME(?, 'yyyy-MM-dd HH:mm:ss'),
                    CASE WHEN ? IS NULL THEN NULL ELSE PARSEDATETIME(?, 'yyyy-MM-dd HH:mm:ss') END,
                    CASE WHEN ? IS NULL THEN NULL ELSE PARSEDATETIME(?, 'yyyy-MM-dd HH:mm:ss') END)
                """,
                id, tenantId, actionType, entityType, entityId, requestedBy, reviewedBy, status,
                payloadJson, pendingRequestKey, requestId, createdAt,
                reviewedAt, reviewedAt, executedAt, executedAt
        );
    }

    private Context context(Connection connection) {
        return new Context() {
            @Override
            public Configuration getConfiguration() {
                return null;
            }

            @Override
            public Connection getConnection() {
                return connection;
            }
        };
    }

    private String userStatusDisableKey(Long tenantId, Long userId) {
        return "USER_STATUS_DISABLE:" + tenantId + ":" + userId;
    }

    private String importJobSelectiveReplayKey(Long tenantId, Long sourceJobId, List<String> errorCodes) {
        return "IMPORT_JOB_SELECTIVE_REPLAY:" + tenantId + ":" + sourceJobId + ":" + md5Hex(String.join("|", errorCodes));
    }

    private String ticketCommentCreateKey(Long tenantId, Long ticketId, String commentContent) {
        return "TICKET_COMMENT_CREATE:" + tenantId + ":" + ticketId + ":" + md5Hex(commentContent);
    }

    private String md5Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("MD5 algorithm unavailable", ex);
        }
    }
}
