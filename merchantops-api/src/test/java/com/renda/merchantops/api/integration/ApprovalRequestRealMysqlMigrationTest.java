package com.renda.merchantops.api.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "MERCHANTOPS_RUN_REAL_MYSQL_MIGRATION_TESTS", matches = "(?i)true")
class ApprovalRequestRealMysqlMigrationTest {

    private static final String JDBC_PARAMS = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";

    @Test
    void flywayShouldApplyV12AndV13AgainstRealMySqlAndPreservePendingRequestKeySemantics() throws Exception {
        MySqlAdminConfig config = loadConfig();
        String databaseName = "approval_request_migration_" + UUID.randomUUID().toString().replace("-", "");
        String adminUrl = "jdbc:mysql://" + config.host() + ":" + config.port() + "/mysql" + JDBC_PARAMS;
        String databaseUrl = "jdbc:mysql://" + config.host() + ":" + config.port() + "/" + databaseName + JDBC_PARAMS;

        try (Connection adminConnection = DriverManager.getConnection(adminUrl, config.username(), config.password())) {
            adminConnection.createStatement().execute("CREATE DATABASE `" + databaseName + "`");
        }

        try {
            migrate(databaseUrl, config, "8");
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(databaseUrl, config.username(), config.password()));
            seedLegacyApprovalRows(jdbcTemplate);

            migrate(databaseUrl, config, "12");
            jdbcTemplate.update(
                    "UPDATE approval_request SET pending_request_key = ? WHERE id = ?",
                    "stale-import-key",
                    9401L
            );
            jdbcTemplate.update(
                    "UPDATE approval_request SET pending_request_key = ? WHERE id = ?",
                    "stale-ticket-key",
                    9402L
            );

            migrate(databaseUrl, config, null);

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT status FROM approval_request WHERE id = ?",
                    String.class,
                    9001L
            )).isEqualTo("REJECTED");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT pending_request_key FROM approval_request WHERE id = ?",
                    String.class,
                    9002L
            )).isEqualTo(userStatusDisableKey(11L, 103L));
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT reviewed_at FROM approval_request WHERE id = ?",
                    Timestamp.class,
                    9001L
            )).isEqualTo(Timestamp.valueOf("2026-03-20 10:00:00"));

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT status FROM approval_request WHERE id = ?",
                    String.class,
                    9101L
            )).isEqualTo("REJECTED");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT pending_request_key FROM approval_request WHERE id = ?",
                    String.class,
                    9102L
            )).isEqualTo(importJobSelectiveReplayKey(11L, 7001L, List.of("INVALID_EMAIL", "UNKNOWN_ROLE")));
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT reviewed_at FROM approval_request WHERE id = ?",
                    Timestamp.class,
                    9101L
            )).isEqualTo(Timestamp.valueOf("2026-03-30 10:00:00"));

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT status FROM approval_request WHERE id = ?",
                    String.class,
                    9201L
            )).isEqualTo("REJECTED");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT pending_request_key FROM approval_request WHERE id = ?",
                    String.class,
                    9202L
            )).isEqualTo(ticketCommentCreateKey(11L, 301L, "Hello store team"));

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

            jdbcTemplate.update(
                    "UPDATE approval_request SET status = ?, pending_request_key = NULL, reviewed_at = CURRENT_TIMESTAMP, executed_at = CURRENT_TIMESTAMP WHERE id = ?",
                    "APPROVED",
                    9002L
            );
            jdbcTemplate.update(
                    """
                    INSERT INTO approval_request (
                        id, tenant_id, action_type, entity_type, entity_id, requested_by, status, payload_json,
                        pending_request_key, request_id, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """,
                    9010L, 11L, "USER_STATUS_DISABLE", "USER", 103L, 101L, "PENDING", "{\"status\":\"DISABLED\"}",
                    userStatusDisableKey(11L, 103L), "disable-req-mysql-after-resolution"
            );
            assertThat(jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM approval_request
                    WHERE tenant_id = ?
                      AND action_type = ?
                      AND entity_type = ?
                      AND entity_id = ?
                      AND status = ?
                    """,
                    Integer.class,
                    11L,
                    "USER_STATUS_DISABLE",
                    "USER",
                    103L,
                    "PENDING"
            )).isEqualTo(1);
        } finally {
            try (Connection adminConnection = DriverManager.getConnection(adminUrl, config.username(), config.password())) {
                adminConnection.createStatement().execute("DROP DATABASE IF EXISTS `" + databaseName + "`");
            }
        }
    }

    private void migrate(String databaseUrl, MySqlAdminConfig config, String targetVersion) {
        var configuration = Flyway.configure()
                .dataSource(databaseUrl, config.username(), config.password())
                .locations("classpath:db/migration");
        if (targetVersion != null) {
            configuration.target(targetVersion);
        }
        Flyway flyway = configuration.load();
        flyway.migrate();
    }

    private void seedLegacyApprovalRows(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update(
                "INSERT INTO tenant (id, tenant_code, tenant_name, status) VALUES (?, ?, ?, ?)",
                11L, "migration-test-shop", "Migration Test Shop", "ACTIVE"
        );
        jdbcTemplate.update(
                "INSERT INTO users (id, tenant_id, username, password_hash, display_name, email, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                101L, 11L, "migration-admin", "$2a$10$C5/1udHsqaaa16O0xqkk0.RJp3vYeWe1ciwQhYT5bnJ2ogCU.QgY2",
                "Migration Admin", "migration-admin@test.local", "ACTIVE"
        );
        jdbcTemplate.update(
                "INSERT INTO users (id, tenant_id, username, password_hash, display_name, email, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                105L, 11L, "migration-reviewer", "$2a$10$C5/1udHsqaaa16O0xqkk0.RJp3vYeWe1ciwQhYT5bnJ2ogCU.QgY2",
                "Migration Reviewer", "migration-reviewer@test.local", "ACTIVE"
        );

        insertApprovalRequest(
                jdbcTemplate,
                9001L,
                11L,
                "USER_STATUS_DISABLE",
                "USER",
                103L,
                101L,
                "PENDING",
                "{\"status\":\"DISABLED\"}",
                "disable-dup-old",
                "2026-03-20 10:00:00",
                null,
                null,
                null
        );
        insertApprovalRequest(
                jdbcTemplate,
                9002L,
                11L,
                "USER_STATUS_DISABLE",
                "USER",
                103L,
                101L,
                "PENDING",
                "{\"status\":\"DISABLED\"}",
                "disable-dup-new",
                "2026-03-20 11:00:00",
                null,
                null,
                null
        );
        insertApprovalRequest(
                jdbcTemplate,
                9101L,
                11L,
                "IMPORT_JOB_SELECTIVE_REPLAY",
                "IMPORT_JOB",
                7001L,
                101L,
                "PENDING",
                "{\"sourceJobId\":7001,\"errorCodes\":[\"UNKNOWN_ROLE\",\"INVALID_EMAIL\",\"UNKNOWN_ROLE\"],\"sourceInteractionId\":9103,\"proposalReason\":\"first\"}",
                "import-proposal-old",
                "2026-03-30 10:00:00",
                null,
                null,
                null
        );
        insertApprovalRequest(
                jdbcTemplate,
                9102L,
                11L,
                "IMPORT_JOB_SELECTIVE_REPLAY",
                "IMPORT_JOB",
                7001L,
                101L,
                "PENDING",
                "{\"sourceJobId\":7001,\"errorCodes\":[\"INVALID_EMAIL\",\"UNKNOWN_ROLE\"],\"sourceInteractionId\":9104,\"proposalReason\":\"second\"}",
                "import-proposal-new",
                "2026-03-30 11:00:00",
                null,
                null,
                null
        );
        insertApprovalRequest(
                jdbcTemplate,
                9201L,
                11L,
                "TICKET_COMMENT_CREATE",
                "TICKET",
                301L,
                101L,
                "PENDING",
                "{\"commentContent\":\"  Hello store team  \",\"sourceInteractionId\":9002}",
                "ticket-proposal-old",
                "2026-03-30 10:30:00",
                null,
                null,
                null
        );
        insertApprovalRequest(
                jdbcTemplate,
                9202L,
                11L,
                "TICKET_COMMENT_CREATE",
                "TICKET",
                301L,
                101L,
                "PENDING",
                "{\"commentContent\":\"Hello store team\"}",
                "ticket-proposal-new",
                "2026-03-30 11:30:00",
                null,
                null,
                null
        );
        insertApprovalRequest(
                jdbcTemplate,
                9401L,
                11L,
                "IMPORT_JOB_SELECTIVE_REPLAY",
                "IMPORT_JOB",
                7003L,
                101L,
                "APPROVED",
                "{\"sourceJobId\":7003,\"errorCodes\":[\"UNKNOWN_ROLE\"]}",
                "import-approved",
                "2026-03-30 09:00:00",
                105L,
                "2026-03-30 09:05:00",
                "2026-03-30 09:05:00"
        );
        insertApprovalRequest(
                jdbcTemplate,
                9402L,
                11L,
                "TICKET_COMMENT_CREATE",
                "TICKET",
                303L,
                101L,
                "REJECTED",
                "{\"commentContent\":\"Already rejected\"}",
                "ticket-rejected",
                "2026-03-30 09:30:00",
                105L,
                "2026-03-30 09:35:00",
                null
        );
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
                                       String requestId,
                                       String createdAt,
                                       Long reviewedBy,
                                       String reviewedAt,
                                       String executedAt) {
        jdbcTemplate.update(
                """
                INSERT INTO approval_request (
                    id, tenant_id, action_type, entity_type, entity_id, requested_by, reviewed_by, status,
                    payload_json, request_id, created_at, reviewed_at, executed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id, tenantId, actionType, entityType, entityId, requestedBy, reviewedBy, status,
                payloadJson, requestId, createdAt, reviewedAt, executedAt
        );
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

    private MySqlAdminConfig loadConfig() {
        return new MySqlAdminConfig(
                readSetting("MERCHANTOPS_TEST_MYSQL_HOST", "localhost"),
                Integer.parseInt(readSetting("MERCHANTOPS_TEST_MYSQL_PORT", "3306")),
                readSetting("MERCHANTOPS_TEST_MYSQL_USER", "root"),
                readSetting("MERCHANTOPS_TEST_MYSQL_PASSWORD", "root")
        );
    }

    private String readSetting(String name, String fallback) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            value = System.getenv(name);
        }
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private record MySqlAdminConfig(String host, int port, String username, String password) {
    }
}
