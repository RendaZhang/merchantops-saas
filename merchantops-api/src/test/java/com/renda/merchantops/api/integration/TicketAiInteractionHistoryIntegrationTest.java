package com.renda.merchantops.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.MerchantOpsApplication;
import com.renda.merchantops.api.support.TestAuthSessionSchemaSupport;
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
                "spring.datasource.url=jdbc:h2:mem:ticketaiinteractions;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driverClassName=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.sql.init.mode=never",
                "spring.flyway.enabled=false",
                "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "jwt.expire-seconds=7200",
                "merchantops.ai.enabled=true",
                "merchantops.ai.provider=OPENAI",
                "merchantops.ai.prompt-version=ticket-summary-v1",
                "merchantops.ai.triage-prompt-version=ticket-triage-v1",
                "merchantops.ai.reply-draft-prompt-version=ticket-reply-draft-v1",
                "merchantops.ai.model-id=gpt-4.1-mini",
                "merchantops.ai.timeout-ms=5000",
                "merchantops.ai.base-url=https://api.openai.com",
                "merchantops.ai.api-key=test-openai-key"
        }
)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TicketAiInteractionHistoryIntegrationTest {

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
        assertThat(currentJdbcUrl()).contains("jdbc:h2:mem:ticketaiinteractions");
        assertThat(currentDatabaseMode()).isEqualTo("MySQL");

        jdbcTemplate.execute("DROP ALL OBJECTS");
        createSchema();

        TestAuthSessionSchemaSupport.createAuthSessionTable(jdbcTemplate);

        seedTenants();
        seedPermissions();
        seedRoles();
        seedUsers();
        seedUserRoles();
        seedRolePermissions();
        seedTickets();
        seedAiInteractions();
    }

    @Test
    void listAiInteractionsShouldReturnHistoryWithStableOrderingPaginationAndNoLeakage() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/tickets/302/ai-interactions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("page", "0")
                        .queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(9003))
                .andExpect(jsonPath("$.data.items[0].interactionType").value("TRIAGE"))
                .andExpect(jsonPath("$.data.items[0].status").value("INVALID_RESPONSE"))
                .andExpect(jsonPath("$.data.items[0].outputSummary").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].promptVersion").value("ticket-triage-v1"))
                .andExpect(jsonPath("$.data.items[0].modelId").value("gpt-4.1-mini"))
                .andExpect(jsonPath("$.data.items[0].latencyMs").value(251))
                .andExpect(jsonPath("$.data.items[0].requestId").value("ticket-ai-triage-invalid-response-1"))
                .andExpect(jsonPath("$.data.items[0].usagePromptTokens").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].usageCompletionTokens").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].usageTotalTokens").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].usageCostMicros").value(nullValue()))
                .andExpect(jsonPath("$.data.items[1].id").value(9002))
                .andExpect(jsonPath("$.data.items[1].interactionType").value("REPLY_DRAFT"))
                .andExpect(jsonPath("$.data.items[1].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.items[1].outputSummary").value("nextStep=Confirm whether the replacement restored printer health and note any blocker before moving toward closure."))
                .andExpect(jsonPath("$.data.items[1].promptVersion").value("ticket-reply-draft-v1"))
                .andExpect(jsonPath("$.data.items[1].modelId").value("gpt-4.1-mini"))
                .andExpect(jsonPath("$.data.items[1].latencyMs").value(436))
                .andExpect(jsonPath("$.data.items[1].requestId").value("ticket-ai-reply-draft-req-1"))
                .andExpect(jsonPath("$.data.items[1].usagePromptTokens").value(140))
                .andExpect(jsonPath("$.data.items[1].usageCompletionTokens").value(88))
                .andExpect(jsonPath("$.data.items[1].usageTotalTokens").value(228))
                .andExpect(jsonPath("$.data.items[1].usageCostMicros").value(2100));

        assertReadOnlyAndNoBusinessMutation(5);
    }

    @Test
    void listAiInteractionsShouldReturnForbiddenWhenTicketReadPermissionMissing() throws Exception {
        jdbcTemplate.update("DELETE FROM role_permission WHERE role_id = ? AND permission_id = ?", 13L, 6L);
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/tickets/302/ai-interactions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("permission denied"));

        assertReadOnlyAndNoBusinessMutation(5);
    }

    @Test
    void listAiInteractionsShouldReturnNotFoundForCrossTenantOrMissingTicket() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/tickets/401/ai-interactions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("ticket not found"));

        mockMvc.perform(get("/api/v1/tickets/999/ai-interactions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("ticket not found"));

        assertReadOnlyAndNoBusinessMutation(5);
    }

    @Test
    void listAiInteractionsShouldFilterByInteractionType() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/tickets/302/ai-interactions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("interactionType", "SUMMARY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(9001))
                .andExpect(jsonPath("$.data.items[0].interactionType").value("SUMMARY"))
                .andExpect(jsonPath("$.data.items[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.items[0].usagePromptTokens").value(120))
                .andExpect(jsonPath("$.data.items[0].usageCompletionTokens").value(52))
                .andExpect(jsonPath("$.data.items[0].usageTotalTokens").value(172))
                .andExpect(jsonPath("$.data.items[0].usageCostMicros").value(1900));

        assertReadOnlyAndNoBusinessMutation(5);
    }

    @Test
    void listAiInteractionsShouldFilterByStatus() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/tickets/302/ai-interactions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("status", "INVALID_RESPONSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(9003))
                .andExpect(jsonPath("$.data.items[0].interactionType").value("TRIAGE"))
                .andExpect(jsonPath("$.data.items[0].status").value("INVALID_RESPONSE"))
                .andExpect(jsonPath("$.data.items[0].usagePromptTokens").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].usageCompletionTokens").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].usageTotalTokens").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].usageCostMicros").value(nullValue()));

        assertReadOnlyAndNoBusinessMutation(5);
    }

    private void assertReadOnlyAndNoBusinessMutation(int expectedAiInteractionCount) {
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_interaction_record", Integer.class))
                .isEqualTo(expectedAiInteractionCount);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ticket WHERE id = ?", String.class, 302L))
                .isEqualTo("IN_PROGRESS");
        assertThat(jdbcTemplate.queryForObject("SELECT assignee_id FROM ticket WHERE id = ?", Long.class, 302L))
                .isEqualTo(102L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket_comment WHERE ticket_id = ?", Integer.class, 302L))
                .isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket_operation_log WHERE ticket_id = ?", Integer.class, 302L))
                .isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_event", Integer.class)).isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM approval_request", Integer.class)).isEqualTo(0);
    }

    private void createSchema() {
        jdbcTemplate.execute("CREATE TABLE tenant (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_code VARCHAR(64) NOT NULL, tenant_name VARCHAR(128) NOT NULL, status VARCHAR(32) NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE users (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, username VARCHAR(64) NOT NULL, password_hash VARCHAR(255) NOT NULL, display_name VARCHAR(128) NOT NULL, email VARCHAR(128), status VARCHAR(32) NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL, created_by BIGINT, updated_by BIGINT, CONSTRAINT uk_users_id_tenant UNIQUE (id, tenant_id))");
        jdbcTemplate.execute("CREATE TABLE `role` (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, role_code VARCHAR(64) NOT NULL, role_name VARCHAR(128) NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE permission (id BIGINT AUTO_INCREMENT PRIMARY KEY, permission_code VARCHAR(64) NOT NULL, permission_name VARCHAR(128) NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE user_role (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, role_id BIGINT NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE role_permission (id BIGINT AUTO_INCREMENT PRIMARY KEY, role_id BIGINT NOT NULL, permission_id BIGINT NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE ticket (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, title VARCHAR(128) NOT NULL, description VARCHAR(2000), status VARCHAR(32) NOT NULL, assignee_id BIGINT, created_by BIGINT NOT NULL, request_id VARCHAR(128) NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE ticket_comment (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, ticket_id BIGINT NOT NULL, content VARCHAR(2000) NOT NULL, created_by BIGINT NOT NULL, request_id VARCHAR(128) NOT NULL, created_at TIMESTAMP NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE ticket_operation_log (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, ticket_id BIGINT NOT NULL, operation_type VARCHAR(64) NOT NULL, detail VARCHAR(512) NOT NULL, operator_id BIGINT NOT NULL, request_id VARCHAR(128) NOT NULL, created_at TIMESTAMP NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE audit_event (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, entity_type VARCHAR(64) NOT NULL, entity_id BIGINT NOT NULL, action_type VARCHAR(64) NOT NULL, operator_id BIGINT NOT NULL, request_id VARCHAR(128) NOT NULL, before_value CLOB, after_value CLOB, approval_status VARCHAR(32) NOT NULL, created_at TIMESTAMP NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE approval_request (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, action_type VARCHAR(64) NOT NULL, entity_type VARCHAR(64) NOT NULL, entity_id BIGINT NOT NULL, requested_by BIGINT NOT NULL, reviewed_by BIGINT, status VARCHAR(32) NOT NULL, payload_json CLOB NOT NULL, pending_request_key VARCHAR(191), request_id VARCHAR(128) NOT NULL, created_at TIMESTAMP NOT NULL, reviewed_at TIMESTAMP, executed_at TIMESTAMP, CONSTRAINT fk_approval_request_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id), CONSTRAINT fk_approval_request_requested_by_tenant FOREIGN KEY (requested_by, tenant_id) REFERENCES users(id, tenant_id), CONSTRAINT fk_approval_request_reviewed_by_tenant FOREIGN KEY (reviewed_by, tenant_id) REFERENCES users(id, tenant_id))");
        jdbcTemplate.execute("CREATE UNIQUE INDEX uk_approval_request_pending_request_key ON approval_request (pending_request_key)");
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
        insertPermission(6L, "TICKET_READ", "Read ticket");
    }

    private void seedRoles() {
        insertRole(11L, 1L, "TENANT_ADMIN", "Tenant Admin");
        insertRole(13L, 1L, "READ_ONLY", "Read Only User");
        insertRole(21L, 2L, "TENANT_ADMIN", "Tenant Admin");
    }

    private void seedUsers() {
        String encodedPassword = passwordEncoder.encode("123456");

        insertUser(101L, 1L, "admin", encodedPassword, "Demo Admin", "admin@demo-shop.local", "ACTIVE");
        insertUser(102L, 1L, "ops", encodedPassword, "Ops User", "ops@demo-shop.local", "ACTIVE");
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
        insertRolePermission(2002L, 11L, 6L);
        insertRolePermission(2003L, 13L, 1L);
        insertRolePermission(2004L, 13L, 6L);
        insertRolePermission(2005L, 21L, 1L);
        insertRolePermission(2006L, 21L, 6L);
    }

    private void seedTickets() {
        insertTicket(302L, 1L, "Printer cable replacement", "Replace damaged cable for store printer.", "IN_PROGRESS", 102L, 101L, "seed-ticket-302");
        insertTicket(303L, 1L, "Store scanner issue", "Scanner intermittently disconnects.", "OPEN", null, 101L, "seed-ticket-303");
        insertTicket(401L, 2L, "Other tenant ticket", "Other tenant issue", "OPEN", null, 201L, "seed-ticket-401");
    }

    private void seedAiInteractions() {
        insertAiInteractionRecord(
                9001L,
                1L,
                103L,
                "ticket-ai-summary-req-1",
                "TICKET",
                302L,
                "SUMMARY",
                "ticket-summary-v1",
                "gpt-4.1-mini",
                "SUCCEEDED",
                412L,
                "Issue: Printer cable replacement is in progress under ops. Current: the ticket is assigned and the latest signal says cable swap started. Next: confirm the replacement outcome and close the ticket if the printer is healthy.",
                120,
                52,
                172,
                1900L,
                LocalDateTime.of(2026, 3, 22, 8, 30)
        );
        insertAiInteractionRecord(
                9002L,
                1L,
                103L,
                "ticket-ai-reply-draft-req-1",
                "TICKET",
                302L,
                "REPLY_DRAFT",
                "ticket-reply-draft-v1",
                "gpt-4.1-mini",
                "SUCCEEDED",
                436L,
                "nextStep=Confirm whether the replacement restored printer health and note any blocker before moving toward closure.",
                140,
                88,
                228,
                2100L,
                LocalDateTime.of(2026, 3, 22, 9, 0)
        );
        insertAiInteractionRecord(
                9003L,
                1L,
                103L,
                "ticket-ai-triage-invalid-response-1",
                "TICKET",
                302L,
                "TRIAGE",
                "ticket-triage-v1",
                "gpt-4.1-mini",
                "INVALID_RESPONSE",
                251L,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 3, 22, 9, 0)
        );
        insertAiInteractionRecord(
                9004L,
                1L,
                103L,
                "ticket-ai-summary-other-ticket-1",
                "TICKET",
                303L,
                "SUMMARY",
                "ticket-summary-v1",
                "gpt-4.1-mini",
                "SUCCEEDED",
                399L,
                "Issue: Scanner intermittently disconnects during checkout.",
                90,
                41,
                131,
                1200L,
                LocalDateTime.of(2026, 3, 22, 7, 45)
        );
        insertAiInteractionRecord(
                9005L,
                2L,
                201L,
                "ticket-ai-summary-other-tenant-1",
                "TICKET",
                401L,
                "SUMMARY",
                "ticket-summary-v1",
                "gpt-4.1-mini",
                "SUCCEEDED",
                405L,
                "Issue: Other tenant issue summary.",
                95,
                37,
                132,
                1300L,
                LocalDateTime.of(2026, 3, 22, 7, 30)
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

    private void insertTicket(Long id,
                              Long tenantId,
                              String title,
                              String description,
                              String status,
                              Long assigneeId,
                              Long createdBy,
                              String requestId) {
        jdbcTemplate.update("""
                INSERT INTO ticket (id, tenant_id, title, description, status, assignee_id, created_by, request_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, id, tenantId, title, description, status, assigneeId, createdBy, requestId);
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
                  \"tenantCode\": \"%s\",
                  \"username\": \"%s\",
                  \"password\": \"%s\"
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
