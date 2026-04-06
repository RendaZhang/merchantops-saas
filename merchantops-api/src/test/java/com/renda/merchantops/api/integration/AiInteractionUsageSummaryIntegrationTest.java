package com.renda.merchantops.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.MerchantOpsApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = MerchantOpsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:aiusagesummary;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driverClassName=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.sql.init.mode=never",
                "spring.flyway.enabled=false",
                "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "jwt.expire-seconds=7200"
        }
)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AiInteractionUsageSummaryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpSchemaAndData() {
        assertThat(currentJdbcUrl()).contains("jdbc:h2:mem:aiusagesummary");
        assertThat(currentDatabaseMode()).isEqualTo("MySQL");

        jdbcTemplate.execute("DROP ALL OBJECTS");
        createSchema();

        seedTenants();
        seedPermissions();
        seedRoles();
        seedUsers();
        seedUserRoles();
        seedRolePermissions();
        seedAiInteractions();
    }

    @Test
    void getUsageSummaryShouldReturnTenantScopedAggregateWithStableBreakdownsAndNoLeakage() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/ai-interactions/usage-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.from").value(nullValue()))
                .andExpect(jsonPath("$.data.to").value(nullValue()))
                .andExpect(jsonPath("$.data.totalInteractions").value(6))
                .andExpect(jsonPath("$.data.succeededCount").value(4))
                .andExpect(jsonPath("$.data.failedCount").value(2))
                .andExpect(jsonPath("$.data.totalPromptTokens").value(530))
                .andExpect(jsonPath("$.data.totalCompletionTokens").value(263))
                .andExpect(jsonPath("$.data.totalTokens").value(793))
                .andExpect(jsonPath("$.data.totalCostMicros").value(6200))
                .andExpect(jsonPath("$.data.byInteractionType[0].interactionType").value("ERROR_SUMMARY"))
                .andExpect(jsonPath("$.data.byInteractionType[0].count").value(2))
                .andExpect(jsonPath("$.data.byInteractionType[0].succeededCount").value(1))
                .andExpect(jsonPath("$.data.byInteractionType[0].failedCount").value(1))
                .andExpect(jsonPath("$.data.byInteractionType[0].totalTokens").value(232))
                .andExpect(jsonPath("$.data.byInteractionType[0].totalCostMicros").value(0))
                .andExpect(jsonPath("$.data.byInteractionType[1].interactionType").value("MAPPING_SUGGESTION"))
                .andExpect(jsonPath("$.data.byInteractionType[2].interactionType").value("REPLY_DRAFT"))
                .andExpect(jsonPath("$.data.byInteractionType[3].interactionType").value("SUMMARY"))
                .andExpect(jsonPath("$.data.byInteractionType[4].interactionType").value("TRIAGE"))
                .andExpect(jsonPath("$.data.byStatus[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.byStatus[0].count").value(4))
                .andExpect(jsonPath("$.data.byStatus[0].totalTokens").value(773))
                .andExpect(jsonPath("$.data.byStatus[0].totalCostMicros").value(6200))
                .andExpect(jsonPath("$.data.byStatus[1].status").value("INVALID_RESPONSE"))
                .andExpect(jsonPath("$.data.byStatus[2].status").value("PROVIDER_TIMEOUT"))
                .andExpect(jsonPath("$.data.byPromptVersion[0].promptVersion").value("import-error-summary-v1"))
                .andExpect(jsonPath("$.data.byPromptVersion[0].count").value(2))
                .andExpect(jsonPath("$.data.byPromptVersion[0].succeededCount").value(1))
                .andExpect(jsonPath("$.data.byPromptVersion[0].failedCount").value(1))
                .andExpect(jsonPath("$.data.byPromptVersion[0].totalTokens").value(232))
                .andExpect(jsonPath("$.data.byPromptVersion[0].totalCostMicros").value(0))
                .andExpect(jsonPath("$.data.byPromptVersion[1].promptVersion").value("import-mapping-suggestion-v1"))
                .andExpect(jsonPath("$.data.byPromptVersion[2].promptVersion").value("ticket-reply-draft-v1"))
                .andExpect(jsonPath("$.data.byPromptVersion[3].promptVersion").value("ticket-summary-v1"))
                .andExpect(jsonPath("$.data.byPromptVersion[4].promptVersion").value("ticket-triage-v1"))
                .andExpect(jsonPath("$.data.requestId").doesNotExist())
                .andExpect(jsonPath("$.data.outputSummary").doesNotExist())
                .andExpect(jsonPath("$.data.promptVersion").doesNotExist())
                .andExpect(jsonPath("$.data.modelId").doesNotExist())
                .andExpect(jsonPath("$.data.rawPrompt").doesNotExist())
                .andExpect(jsonPath("$.data.providerPayload").doesNotExist())
                .andExpect(jsonPath("$.data.byInteractionType[0].requestId").doesNotExist())
                .andExpect(jsonPath("$.data.byStatus[0].promptVersion").doesNotExist())
                .andExpect(jsonPath("$.data.byPromptVersion[0].requestId").doesNotExist())
                .andExpect(jsonPath("$.data.byPromptVersion[0].outputSummary").doesNotExist())
                .andExpect(jsonPath("$.data.byPromptVersion[0].modelId").doesNotExist());

        assertReadOnlyAndNoMutation(7);
    }

    @Test
    void getUsageSummaryShouldReturnForbiddenWhenUserReadPermissionMissing() throws Exception {
        jdbcTemplate.update("DELETE FROM role_permission WHERE role_id = ? AND permission_id = ?", 13L, 1L);
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/ai-interactions/usage-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("permission denied"));

        assertReadOnlyAndNoMutation(7);
    }

    @Test
    void getUsageSummaryShouldApplyInclusiveFromAndToFiltering() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/ai-interactions/usage-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("from", "2026-04-01T00:00:00")
                        .queryParam("to", "2026-04-04T13:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.from").value("2026-04-01T00:00:00"))
                .andExpect(jsonPath("$.data.to").value("2026-04-04T13:00:00"))
                .andExpect(jsonPath("$.data.totalInteractions").value(5))
                .andExpect(jsonPath("$.data.succeededCount").value(3))
                .andExpect(jsonPath("$.data.failedCount").value(2))
                .andExpect(jsonPath("$.data.totalPromptTokens").value(400))
                .andExpect(jsonPath("$.data.totalCompletionTokens").value(232))
                .andExpect(jsonPath("$.data.totalTokens").value(632))
                .andExpect(jsonPath("$.data.totalCostMicros").value(4000))
                .andExpect(jsonPath("$.data.byInteractionType[0].interactionType").value("ERROR_SUMMARY"))
                .andExpect(jsonPath("$.data.byInteractionType[0].count").value(2))
                .andExpect(jsonPath("$.data.byPromptVersion[0].promptVersion").value("import-error-summary-v1"))
                .andExpect(jsonPath("$.data.byPromptVersion[0].count").value(2))
                .andExpect(jsonPath("$.data.byPromptVersion[1].promptVersion").value("ticket-reply-draft-v1"))
                .andExpect(jsonPath("$.data.byPromptVersion[2].promptVersion").value("ticket-summary-v1"))
                .andExpect(jsonPath("$.data.byPromptVersion[3].promptVersion").value("ticket-triage-v1"));

        assertReadOnlyAndNoMutation(7);
    }

    @Test
    void getUsageSummaryShouldFilterByEntityType() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/ai-interactions/usage-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("entityType", "IMPORT_JOB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalInteractions").value(3))
                .andExpect(jsonPath("$.data.succeededCount").value(2))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.totalTokens").value(393))
                .andExpect(jsonPath("$.data.totalCostMicros").value(2200))
                .andExpect(jsonPath("$.data.byInteractionType[0].interactionType").value("ERROR_SUMMARY"))
                .andExpect(jsonPath("$.data.byInteractionType[1].interactionType").value("MAPPING_SUGGESTION"))
                .andExpect(jsonPath("$.data.byPromptVersion[0].promptVersion").value("import-error-summary-v1"))
                .andExpect(jsonPath("$.data.byPromptVersion[1].promptVersion").value("import-mapping-suggestion-v1"));

        assertReadOnlyAndNoMutation(7);
    }

    @Test
    void getUsageSummaryShouldFilterByInteractionTypeAndStatus() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/ai-interactions/usage-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("interactionType", "ERROR_SUMMARY")
                        .queryParam("status", "SUCCEEDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalInteractions").value(1))
                .andExpect(jsonPath("$.data.succeededCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andExpect(jsonPath("$.data.totalPromptTokens").value(130))
                .andExpect(jsonPath("$.data.totalCompletionTokens").value(82))
                .andExpect(jsonPath("$.data.totalTokens").value(212))
                .andExpect(jsonPath("$.data.totalCostMicros").value(0))
                .andExpect(jsonPath("$.data.byInteractionType[0].interactionType").value("ERROR_SUMMARY"))
                .andExpect(jsonPath("$.data.byStatus[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.byPromptVersion[0].promptVersion").value("import-error-summary-v1"))
                .andExpect(jsonPath("$.data.byPromptVersion[0].count").value(1))
                .andExpect(jsonPath("$.data.byPromptVersion[0].succeededCount").value(1))
                .andExpect(jsonPath("$.data.byPromptVersion[0].failedCount").value(0))
                .andExpect(jsonPath("$.data.byPromptVersion[0].totalTokens").value(212))
                .andExpect(jsonPath("$.data.byPromptVersion[0].totalCostMicros").value(0));

        assertReadOnlyAndNoMutation(7);
    }

    private void assertReadOnlyAndNoMutation(int expectedAiInteractionCount) {
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_interaction_record", Integer.class))
                .isEqualTo(expectedAiInteractionCount);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_event", Integer.class)).isEqualTo(0);
    }

    private void createSchema() {
        jdbcTemplate.execute("CREATE TABLE tenant (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_code VARCHAR(64) NOT NULL, tenant_name VARCHAR(128) NOT NULL, status VARCHAR(32) NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE users (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, username VARCHAR(64) NOT NULL, password_hash VARCHAR(255) NOT NULL, display_name VARCHAR(128) NOT NULL, email VARCHAR(128), status VARCHAR(32) NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL, created_by BIGINT, updated_by BIGINT, CONSTRAINT uk_users_id_tenant UNIQUE (id, tenant_id))");
        jdbcTemplate.execute("CREATE TABLE `role` (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, role_code VARCHAR(64) NOT NULL, role_name VARCHAR(128) NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE permission (id BIGINT AUTO_INCREMENT PRIMARY KEY, permission_code VARCHAR(64) NOT NULL, permission_name VARCHAR(128) NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE user_role (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, role_id BIGINT NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE role_permission (id BIGINT AUTO_INCREMENT PRIMARY KEY, role_id BIGINT NOT NULL, permission_id BIGINT NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE audit_event (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, entity_type VARCHAR(64) NOT NULL, entity_id BIGINT NOT NULL, action_type VARCHAR(64) NOT NULL, operator_id BIGINT NOT NULL, request_id VARCHAR(128) NOT NULL, before_value CLOB, after_value CLOB, approval_status VARCHAR(32) NOT NULL, created_at TIMESTAMP NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE ai_interaction_record (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, user_id BIGINT NOT NULL, request_id VARCHAR(128) NOT NULL, entity_type VARCHAR(64) NOT NULL, entity_id BIGINT NOT NULL, interaction_type VARCHAR(64) NOT NULL, prompt_version VARCHAR(128) NOT NULL, model_id VARCHAR(128), status VARCHAR(32) NOT NULL, latency_ms BIGINT NOT NULL, output_summary CLOB, usage_prompt_tokens INT, usage_completion_tokens INT, usage_total_tokens INT, usage_cost_micros BIGINT, created_at TIMESTAMP NOT NULL, CONSTRAINT fk_ai_interaction_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id), CONSTRAINT fk_ai_interaction_user_tenant FOREIGN KEY (user_id, tenant_id) REFERENCES users(id, tenant_id))");
    }

    private void seedTenants() {
        jdbcTemplate.update("""
                INSERT INTO tenant (id, tenant_code, tenant_name, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, 1L, "demo-shop", "Demo Shop", "ACTIVE");
        jdbcTemplate.update("""
                INSERT INTO tenant (id, tenant_code, tenant_name, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, 2L, "other-shop", "Other Shop", "ACTIVE");
    }

    private void seedPermissions() {
        insertPermission(1L, "USER_READ", "Read user");
    }

    private void seedRoles() {
        insertRole(11L, 1L, "TENANT_ADMIN", "Tenant Admin");
        insertRole(13L, 1L, "READ_ONLY", "Read Only User");
        insertRole(21L, 2L, "TENANT_ADMIN", "Tenant Admin");
    }

    private void seedUsers() {
        String encodedPassword = passwordEncoder.encode("123456");

        insertUser(101L, 1L, "admin", encodedPassword, "Demo Admin", "admin@demo-shop.local", "ACTIVE");
        insertUser(103L, 1L, "viewer", encodedPassword, "Viewer User", "viewer@demo-shop.local", "ACTIVE");
        insertUser(201L, 2L, "outsider", encodedPassword, "Other Tenant User", "outsider@other-shop.local", "ACTIVE");
    }

    private void seedUserRoles() {
        insertUserRole(1001L, 101L, 11L);
        insertUserRole(1002L, 103L, 13L);
        insertUserRole(1003L, 201L, 21L);
    }

    private void seedRolePermissions() {
        insertRolePermission(2001L, 11L, 1L);
        insertRolePermission(2002L, 13L, 1L);
        insertRolePermission(2003L, 21L, 1L);
    }

    private void seedAiInteractions() {
        insertAiInteractionRecord(
                10001L, 1L, 103L, "ticket-summary-1", "TICKET", 302L, "SUMMARY", "ticket-summary-v1",
                "gpt-4.1-mini", "SUCCEEDED", 412L, "Issue summary", 120, 52, 172, 1900L,
                LocalDateTime.of(2026, 4, 1, 10, 0)
        );
        insertAiInteractionRecord(
                10002L, 1L, 103L, "ticket-reply-draft-1", "TICKET", 302L, "REPLY_DRAFT", "ticket-reply-draft-v1",
                "gpt-4.1-mini", "SUCCEEDED", 436L, "reply draft summary", 140, 88, 228, 2100L,
                LocalDateTime.of(2026, 4, 2, 10, 0)
        );
        insertAiInteractionRecord(
                10003L, 1L, 103L, "ticket-triage-invalid-1", "TICKET", 302L, "TRIAGE", "ticket-triage-v1",
                "gpt-4.1-mini", "INVALID_RESPONSE", 251L, null, null, null, null, null,
                LocalDateTime.of(2026, 4, 2, 11, 0)
        );
        insertAiInteractionRecord(
                10004L, 1L, 103L, "import-error-summary-1", "IMPORT_JOB", 7001L, "ERROR_SUMMARY", "import-error-summary-v1",
                "gpt-4.1-mini", "SUCCEEDED", 512L, "error summary", 130, 82, 212, null,
                LocalDateTime.of(2026, 4, 3, 12, 0)
        );
        insertAiInteractionRecord(
                10005L, 1L, 103L, "import-error-summary-timeout-1", "IMPORT_JOB", 7001L, "ERROR_SUMMARY", "import-error-summary-v1",
                "gpt-4.1-mini", "PROVIDER_TIMEOUT", 5000L, null, 10, 10, 20, null,
                LocalDateTime.of(2026, 4, 4, 13, 0)
        );
        insertAiInteractionRecord(
                10006L, 1L, 103L, "import-mapping-suggestion-1", "IMPORT_JOB", 7002L, "MAPPING_SUGGESTION", "import-mapping-suggestion-v1",
                "gpt-4.1-mini", "SUCCEEDED", 544L, "mapping suggestion", 130, 31, 161, 2200L,
                LocalDateTime.of(2026, 3, 31, 23, 0)
        );
        insertAiInteractionRecord(
                20001L, 2L, 201L, "other-tenant-summary-1", "TICKET", 401L, "SUMMARY", "ticket-summary-v1",
                "gpt-4.1-mini", "SUCCEEDED", 399L, "other tenant summary", 999, 999, 1998, 9999L,
                LocalDateTime.of(2026, 4, 2, 8, 0)
        );
    }

    private void insertPermission(Long id, String permissionCode, String permissionName) {
        jdbcTemplate.update("""
                INSERT INTO permission (id, permission_code, permission_name, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, id, permissionCode, permissionName);
    }

    private void insertRole(Long id, Long tenantId, String roleCode, String roleName) {
        jdbcTemplate.update("""
                INSERT INTO `role` (id, tenant_id, role_code, role_name, created_at, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, id, tenantId, roleCode, roleName);
    }

    private void insertUser(Long id,
                            Long tenantId,
                            String username,
                            String passwordHash,
                            String displayName,
                            String email,
                            String status) {
        jdbcTemplate.update("""
                INSERT INTO users (id, tenant_id, username, password_hash, display_name, email, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, id, tenantId, username, passwordHash, displayName, email, status);
    }

    private void insertUserRole(Long id, Long userId, Long roleId) {
        jdbcTemplate.update("""
                INSERT INTO user_role (id, user_id, role_id)
                VALUES (?, ?, ?)
                """, id, userId, roleId);
    }

    private void insertRolePermission(Long id, Long roleId, Long permissionId) {
        jdbcTemplate.update("""
                INSERT INTO role_permission (id, role_id, permission_id)
                VALUES (?, ?, ?)
                """, id, roleId, permissionId);
    }

    private void insertAiInteractionRecord(Long id,
                                           Long tenantId,
                                           Long userId,
                                           String requestId,
                                           String entityType,
                                           Long entityId,
                                           String interactionType,
                                           String promptVersion,
                                           String modelId,
                                           String status,
                                           Long latencyMs,
                                           String outputSummary,
                                           Integer usagePromptTokens,
                                           Integer usageCompletionTokens,
                                           Integer usageTotalTokens,
                                           Long usageCostMicros,
                                           LocalDateTime createdAt) {
        jdbcTemplate.update("""
                INSERT INTO ai_interaction_record (
                    id,
                    tenant_id,
                    user_id,
                    request_id,
                    entity_type,
                    entity_id,
                    interaction_type,
                    prompt_version,
                    model_id,
                    status,
                    latency_ms,
                    output_summary,
                    usage_prompt_tokens,
                    usage_completion_tokens,
                    usage_total_tokens,
                    usage_cost_micros,
                    created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                tenantId,
                userId,
                requestId,
                entityType,
                entityId,
                interactionType,
                promptVersion,
                modelId,
                status,
                latencyMs,
                outputSummary,
                usagePromptTokens,
                usageCompletionTokens,
                usageTotalTokens,
                usageCostMicros,
                Timestamp.valueOf(createdAt)
        );
    }

    private String loginAndGetToken(String tenantCode, String username, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(loginRequest(tenantCode, username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andReturn();

        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsByteArray());
        String token = root.path("data").path("accessToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private String loginRequest(String tenantCode, String username, String password) {
        return """
                {
                  "tenantCode": "%s",
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(tenantCode, username, password);
    }

    private String bearerToken(String token) {
        return "Bearer " + token;
    }

    private String currentJdbcUrl() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getURL();
        } catch (SQLException ex) {
            throw new IllegalStateException("failed to inspect test datasource url", ex);
        }
    }

    private String currentDatabaseMode() {
        return jdbcTemplate.queryForObject(
                "SELECT SETTING_VALUE FROM INFORMATION_SCHEMA.SETTINGS WHERE SETTING_NAME = 'MODE'",
                String.class
        );
    }
}
