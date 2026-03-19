package com.renda.merchantops.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.MerchantOpsApplication;
import com.renda.merchantops.api.messaging.ImportJobPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = MerchantOpsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:importauthz;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driverClassName=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.sql.init.mode=never",
                "spring.flyway.enabled=false",
                "spring.task.scheduling.enabled=false",
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "jwt.expire-seconds=7200",
                "merchantops.import.storage.local-dir=target/test-import-authz"
        }
)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ImportJobAuthzIntegrationTest {

    private static final Path STORAGE_ROOT = Path.of("target/test-import-authz");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private ImportJobPublisher importJobPublisher;

    @BeforeEach
    void setUpSchemaAndData() throws Exception {
        jdbcTemplate.execute("DROP ALL OBJECTS");
        jdbcTemplate.execute("""
                CREATE TABLE tenant (
                    id BIGINT PRIMARY KEY,
                    tenant_code VARCHAR(64) NOT NULL,
                    tenant_name VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE users (
                    id BIGINT PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    username VARCHAR(64) NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    display_name VARCHAR(128) NOT NULL,
                    email VARCHAR(128),
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL,
                    created_by BIGINT,
                    updated_by BIGINT,
                    CONSTRAINT uk_users_id_tenant UNIQUE (id, tenant_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE `role` (
                    id BIGINT PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    role_code VARCHAR(64) NOT NULL,
                    role_name VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE permission (
                    id BIGINT PRIMARY KEY,
                    permission_code VARCHAR(64) NOT NULL,
                    permission_name VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE user_role (
                    id BIGINT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    role_id BIGINT NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE role_permission (
                    id BIGINT PRIMARY KEY,
                    role_id BIGINT NOT NULL,
                    permission_id BIGINT NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE audit_event (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    entity_type VARCHAR(64) NOT NULL,
                    entity_id BIGINT NOT NULL,
                    action_type VARCHAR(64) NOT NULL,
                    operator_id BIGINT NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    before_value CLOB,
                    after_value CLOB,
                    approval_status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE import_job (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    import_type VARCHAR(64) NOT NULL,
                    source_type VARCHAR(32) NOT NULL,
                    source_filename VARCHAR(255) NOT NULL,
                    storage_key VARCHAR(512) NOT NULL,
                    source_job_id BIGINT,
                    status VARCHAR(32) NOT NULL,
                    requested_by BIGINT NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    total_count INT NOT NULL,
                    success_count INT NOT NULL,
                    failure_count INT NOT NULL,
                    error_summary VARCHAR(512),
                    created_at TIMESTAMP NOT NULL,
                    started_at TIMESTAMP,
                    finished_at TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE import_job_item_error (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    import_job_id BIGINT NOT NULL,
                    source_row_number INT,
                    error_code VARCHAR(64) NOT NULL,
                    error_message VARCHAR(512) NOT NULL,
                    raw_payload CLOB,
                    created_at TIMESTAMP NOT NULL
                )
                """);

        String encodedPassword = passwordEncoder.encode("123456");
        jdbcTemplate.update("""
                INSERT INTO tenant (id, tenant_code, tenant_name, status, created_at, updated_at)
                VALUES (1, 'demo-shop', 'Demo Shop', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO tenant (id, tenant_code, tenant_name, status, created_at, updated_at)
                VALUES (2, 'other-shop', 'Other Shop', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO users (id, tenant_id, username, password_hash, display_name, email, status, created_at, updated_at, created_by, updated_by)
                VALUES (101, 1, 'admin', ?, 'Demo Admin', 'admin@demo-shop.local', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 101, 101)
                """, encodedPassword);
        jdbcTemplate.update("""
                INSERT INTO users (id, tenant_id, username, password_hash, display_name, email, status, created_at, updated_at, created_by, updated_by)
                VALUES (103, 1, 'viewer', ?, 'Viewer User', 'viewer@demo-shop.local', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 101, 101)
                """, encodedPassword);
        jdbcTemplate.update("""
                INSERT INTO users (id, tenant_id, username, password_hash, display_name, email, status, created_at, updated_at, created_by, updated_by)
                VALUES (201, 2, 'outsider', ?, 'Outsider User', 'outsider@other-shop.local', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 201, 201)
                """, encodedPassword);

        jdbcTemplate.update("""
                INSERT INTO `role` (id, tenant_id, role_code, role_name, created_at, updated_at)
                VALUES (11, 1, 'TENANT_ADMIN', 'Tenant Admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO `role` (id, tenant_id, role_code, role_name, created_at, updated_at)
                VALUES (13, 1, 'READ_ONLY', 'Read Only', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO permission (id, permission_code, permission_name, created_at, updated_at)
                VALUES (1, 'USER_READ', 'Read user', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO permission (id, permission_code, permission_name, created_at, updated_at)
                VALUES (2, 'USER_WRITE', 'Write user', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("INSERT INTO user_role (id, user_id, role_id) VALUES (1001, 101, 11)");
        jdbcTemplate.update("INSERT INTO user_role (id, user_id, role_id) VALUES (1003, 103, 13)");
        jdbcTemplate.update("INSERT INTO role_permission (id, role_id, permission_id) VALUES (2001, 11, 1)");
        jdbcTemplate.update("INSERT INTO role_permission (id, role_id, permission_id) VALUES (2002, 11, 2)");
        jdbcTemplate.update("INSERT INTO role_permission (id, role_id, permission_id) VALUES (2013, 13, 1)");

        if (Files.exists(STORAGE_ROOT)) {
            try (var walk = Files.walk(STORAGE_ROOT)) {
                walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                });
            }
        }
        Files.createDirectories(STORAGE_ROOT.resolve("1"));
        Files.writeString(
                STORAGE_ROOT.resolve("1/source-authz.csv"),
                """
                        username,displayName,email,password,roleCodes
                        retry-role,Retry Role,retry-role@example.com,abc123,RETRY_ROLE
                        retry-email,Retry Email,bad-email,abc123,READ_ONLY
                        """,
                StandardCharsets.UTF_8
        );

        jdbcTemplate.update("""
                INSERT INTO import_job (
                    id, tenant_id, import_type, source_type, source_filename, storage_key, source_job_id, status,
                    requested_by, request_id, total_count, success_count, failure_count, error_summary, created_at, started_at, finished_at
                )
                VALUES (
                    7001, 1, 'USER_CSV', 'CSV', 'source-authz.csv', '1/source-authz.csv', NULL, 'FAILED',
                    101, 'req-source-authz', 2, 0, 2, 'all rows failed validation', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7101, 1, 7001, 2, 'UNKNOWN_ROLE', 'roleCodes must exist in current tenant',
                        'retry-role,Retry Role,retry-role@example.com,abc123,RETRY_ROLE', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7102, 1, 7001, 3, 'INVALID_EMAIL', 'email must be a valid email',
                        'retry-email,Retry Email,bad-email,abc123,READ_ONLY', CURRENT_TIMESTAMP)
                """);
    }

    @Test
    void importWriteEndpointsShouldRequireUserWriteAndPersistTenantScopedRows() throws Exception {
        String readToken = loginAndGetToken("demo-shop", "viewer", "123456");
        String writeToken = loginAndGetToken("demo-shop", "admin", "123456");

        mockMvc.perform(multipart("/api/v1/import-jobs")
                        .file(importCreateRequestPart())
                        .file(importCsvFilePart())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(readToken))
                        .header("X-Request-Id", "req-authz-create-read"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("permission denied"));

        mockMvc.perform(multipart("/api/v1/import-jobs")
                        .file(importCreateRequestPart())
                        .file(importCsvFilePart())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(writeToken))
                        .header("X-Request-Id", "req-authz-create-write"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(post("/api/v1/import-jobs/7001/replay-failures")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(readToken))
                        .header("X-Request-Id", "req-authz-replay-failures-read"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("permission denied"));

        mockMvc.perform(post("/api/v1/import-jobs/7001/replay-failures")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(writeToken))
                        .header("X-Request-Id", "req-authz-replay-failures-write"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(post("/api/v1/import-jobs/7001/replay-file")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(readToken))
                        .header("X-Request-Id", "req-authz-replay-file-read"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("permission denied"));

        mockMvc.perform(post("/api/v1/import-jobs/7001/replay-file")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(writeToken))
                        .header("X-Request-Id", "req-authz-replay-file-write"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(post("/api/v1/import-jobs/7001/replay-failures/selective")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"errorCodes":["UNKNOWN_ROLE"]}
                                """)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(readToken))
                        .header("X-Request-Id", "req-authz-replay-selective-read"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("permission denied"));

        mockMvc.perform(post("/api/v1/import-jobs/7001/replay-failures/selective")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"errorCodes":["UNKNOWN_ROLE"]}
                                """)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(writeToken))
                        .header("X-Request-Id", "req-authz-replay-selective-write"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(post("/api/v1/import-jobs/7001/replay-failures/edited")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "errorId": 7101,
                                      "username": "retry-role-fixed",
                                      "displayName": "Retry Role Fixed",
                                      "email": "retry-role-fixed@example.com",
                                      "password": "abc123",
                                      "roleCodes": ["READ_ONLY"]
                                    },
                                    {
                                      "errorId": 7102,
                                      "username": "retry-email-fixed",
                                      "displayName": "Retry Email Fixed",
                                      "email": "retry-email-fixed@example.com",
                                      "password": "abc123",
                                      "roleCodes": ["READ_ONLY"]
                                    }
                                  ]
                                }
                                """)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(readToken))
                        .header("X-Request-Id", "req-authz-replay-edited-read"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("permission denied"));

        mockMvc.perform(post("/api/v1/import-jobs/7001/replay-failures/edited")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "errorId": 7101,
                                      "username": "retry-role-fixed",
                                      "displayName": "Retry Role Fixed",
                                      "email": "retry-role-fixed@example.com",
                                      "password": "abc123",
                                      "roleCodes": ["READ_ONLY"]
                                    },
                                    {
                                      "errorId": 7102,
                                      "username": "retry-email-fixed",
                                      "displayName": "Retry Email Fixed",
                                      "email": "retry-email-fixed@example.com",
                                      "password": "abc123",
                                      "roleCodes": ["READ_ONLY"]
                                    }
                                  ]
                                }
                                """)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(writeToken))
                        .header("X-Request-Id", "req-authz-replay-edited-write"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        Integer writeCreateCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM import_job WHERE tenant_id = 1 AND request_id = 'req-authz-create-write'",
                Integer.class
        );
        Integer writeReplayCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM import_job WHERE tenant_id = 1 AND request_id IN ('req-authz-replay-failures-write', 'req-authz-replay-file-write', 'req-authz-replay-selective-write', 'req-authz-replay-edited-write')",
                Integer.class
        );
        Integer replaySourceScopedRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM import_job WHERE tenant_id = 1 AND source_job_id = 7001",
                Integer.class
        );
        Integer crossTenantRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM import_job WHERE tenant_id = 2",
                Integer.class
        );
        assertThat(writeCreateCount).isEqualTo(1);
        assertThat(writeReplayCount).isEqualTo(4);
        assertThat(replaySourceScopedRows).isEqualTo(4);
        assertThat(crossTenantRows).isZero();
    }

    private MockMultipartFile importCreateRequestPart() {
        return new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                """
                        {"importType":"USER_CSV"}
                        """.getBytes(StandardCharsets.UTF_8)
        );
    }

    private MockMultipartFile importCsvFilePart() {
        return new MockMultipartFile(
                "file",
                "users-authz-create.csv",
                "text/csv",
                """
                        username,displayName,email,password,roleCodes
                        create-user,Create User,create-user@example.com,abc123,READ_ONLY
                        """.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String loginAndGetToken(String tenantCode, String username, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantCode": "%s",
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(tenantCode, username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andReturn();
        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsByteArray());
        return root.path("data").path("accessToken").asText();
    }

    private String bearerToken(String token) {
        return "Bearer " + token;
    }
}
