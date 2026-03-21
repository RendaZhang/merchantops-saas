package com.renda.merchantops.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.MerchantOpsApplication;
import com.renda.merchantops.api.ai.AiProviderException;
import com.renda.merchantops.api.ai.AiProviderFailureType;
import com.renda.merchantops.api.ai.TicketReplyDraftAiProvider;
import com.renda.merchantops.api.ai.TicketReplyDraftProviderRequest;
import com.renda.merchantops.api.ai.TicketReplyDraftProviderResult;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.filter.RequestIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = MerchantOpsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:ticketaireplydraft;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driverClassName=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.sql.init.mode=never",
                "spring.flyway.enabled=false",
                "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "jwt.expire-seconds=7200",
                "merchantops.ai.enabled=true",
                "merchantops.ai.prompt-version=ticket-summary-v1",
                "merchantops.ai.triage-prompt-version=ticket-triage-v1",
                "merchantops.ai.reply-draft-prompt-version=ticket-reply-draft-v1",
                "merchantops.ai.model-id=gpt-4.1-mini",
                "merchantops.ai.timeout-ms=5000",
                "merchantops.ai.openai.api-key=test-openai-key"
        }
)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TicketAiReplyDraftIntegrationTest {

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

    @Autowired
    private AiProperties aiProperties;

    @MockBean
    private TicketReplyDraftAiProvider ticketReplyDraftAiProvider;

    @BeforeEach
    void setUpSchemaAndData() {
        assertThat(currentJdbcUrl()).contains("jdbc:h2:mem:ticketaireplydraft");
        assertThat(currentDatabaseMode()).isEqualTo("MySQL");

        reset(ticketReplyDraftAiProvider);
        aiProperties.setEnabled(true);
        aiProperties.setPromptVersion("ticket-summary-v1");
        aiProperties.setTriagePromptVersion("ticket-triage-v1");
        aiProperties.setReplyDraftPromptVersion("ticket-reply-draft-v1");
        aiProperties.setModelId("gpt-4.1-mini");
        aiProperties.setTimeoutMs(5000);
        aiProperties.getOpenai().setApiKey("test-openai-key");
        aiProperties.getOpenai().setBaseUrl("https://api.openai.com");

        jdbcTemplate.execute("DROP ALL OBJECTS");
        createSchema();
        seedTenants();
        seedPermissions();
        seedRoles();
        seedUsers();
        seedUserRoles();
        seedRolePermissions();
        seedTickets();
        seedComments();
        seedOperationLogs();
    }

    @Test
    void aiReplyDraftShouldReturnDraftForAuthorizedViewerAndWriteInteractionRecordWithoutBusinessSideEffects() throws Exception {
        when(ticketReplyDraftAiProvider.generateReplyDraft(any())).thenReturn(new TicketReplyDraftProviderResult(
                "Quick update from ops.",
                "The ticket is still in progress and the latest ticket activity confirms the cable swap has started for the store printer issue.",
                "Confirm whether the replacement restored printer health and note any blocker before moving toward closure.",
                "I will add another internal update once the verification result is confirmed.",
                "gpt-4.1-mini",
                146,
                64,
                210,
                null
        ));
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/tickets/302/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-ai-reply-draft-req-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.ticketId").value(302))
                .andExpect(jsonPath("$.data.promptVersion").value("ticket-reply-draft-v1"))
                .andExpect(jsonPath("$.data.modelId").value("gpt-4.1-mini"))
                .andExpect(jsonPath("$.data.requestId").value("ticket-ai-reply-draft-req-1"))
                .andExpect(jsonPath("$.data.opening").value("Quick update from ops."))
                .andExpect(jsonPath("$.data.nextStep").value("Confirm whether the replacement restored printer health and note any blocker before moving toward closure."))
                .andExpect(jsonPath("$.data.draftText").value("Quick update from ops.\n\nThe ticket is still in progress and the latest ticket activity confirms the cable swap has started for the store printer issue.\n\nNext step: Confirm whether the replacement restored printer health and note any blocker before moving toward closure.\n\nI will add another internal update once the verification result is confirmed."))
                .andExpect(jsonPath("$.data.latencyMs").isNumber());

        ArgumentCaptor<TicketReplyDraftProviderRequest> requestCaptor = ArgumentCaptor.forClass(TicketReplyDraftProviderRequest.class);
        verify(ticketReplyDraftAiProvider).generateReplyDraft(requestCaptor.capture());
        assertThat(requestCaptor.getValue().ticketId()).isEqualTo(302L);
        assertThat(requestCaptor.getValue().prompt().userPrompt()).contains("Cable swap started.");
        assertThat(requestCaptor.getValue().prompt().userPrompt()).contains("status changed from OPEN to IN_PROGRESS");

        assertNoTicketWorkflowMutation();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_interaction_record", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT interaction_type FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("REPLY_DRAFT");
        assertThat(jdbcTemplate.queryForObject("SELECT prompt_version FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("ticket-reply-draft-v1");
        assertThat(jdbcTemplate.queryForObject("SELECT model_id FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("gpt-4.1-mini");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("SUCCEEDED");
        assertThat(jdbcTemplate.queryForObject("SELECT output_summary FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("nextStep=Confirm whether the replacement restored printer health and note any blocker before moving toward closure.");
        assertThat(jdbcTemplate.queryForObject("SELECT usage_total_tokens FROM ai_interaction_record WHERE entity_id = ?", Integer.class, 302L)).isEqualTo(210);
    }

    @Test
    void aiReplyDraftShouldReturnForbiddenWhenTicketReadPermissionMissing() throws Exception {
        jdbcTemplate.update("DELETE FROM role_permission WHERE role_id = ? AND permission_id = ?", 13L, 6L);
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/tickets/302/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-ai-reply-draft-no-read-1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("permission denied"));
    }

    @Test
    void aiReplyDraftShouldReturnNotFoundForCrossTenantTicket() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/tickets/401/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-ai-reply-draft-cross-tenant-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("ticket not found"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_interaction_record", Integer.class)).isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_event", Integer.class)).isEqualTo(0);
    }

    @Test
    void aiReplyDraftShouldReturnServiceUnavailableWhenProviderIsNotConfigured() throws Exception {
        aiProperties.setModelId(null);
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/tickets/302/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-ai-reply-draft-not-configured-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("ticket ai reply draft is unavailable"));

        verifyNoInteractions(ticketReplyDraftAiProvider);
        assertThat(jdbcTemplate.queryForObject("SELECT interaction_type FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("REPLY_DRAFT");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("PROVIDER_NOT_CONFIGURED");
    }

    @Test
    void aiReplyDraftShouldReturnServiceUnavailableWhenProviderTimesOut() throws Exception {
        when(ticketReplyDraftAiProvider.generateReplyDraft(any())).thenThrow(new AiProviderException(AiProviderFailureType.TIMEOUT, "timeout"));
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/tickets/302/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-ai-reply-draft-timeout-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("ticket ai reply draft timed out"));

        assertThat(jdbcTemplate.queryForObject("SELECT interaction_type FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("REPLY_DRAFT");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("PROVIDER_TIMEOUT");
        assertThat(jdbcTemplate.queryForObject("SELECT output_summary FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isNull();
    }

    @Test
    void aiReplyDraftShouldReturnServiceUnavailableWhenProviderIsUnavailable() throws Exception {
        when(ticketReplyDraftAiProvider.generateReplyDraft(any())).thenThrow(new AiProviderException(AiProviderFailureType.UNAVAILABLE, "unavailable"));
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/tickets/302/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-ai-reply-draft-provider-unavailable-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("ticket ai reply draft is unavailable"));

        assertThat(jdbcTemplate.queryForObject("SELECT interaction_type FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("REPLY_DRAFT");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("PROVIDER_UNAVAILABLE");
        assertThat(jdbcTemplate.queryForObject("SELECT output_summary FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isNull();
    }

    @Test
    void aiReplyDraftShouldReturnServiceUnavailableWhenFeatureDisabled() throws Exception {
        aiProperties.setEnabled(false);
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/tickets/302/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-ai-reply-draft-disabled-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("ticket ai reply draft is disabled"));

        verifyNoInteractions(ticketReplyDraftAiProvider);
        assertThat(jdbcTemplate.queryForObject("SELECT interaction_type FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("REPLY_DRAFT");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("FEATURE_DISABLED");
    }

    @Test
    void aiReplyDraftShouldReturnServiceUnavailableWhenAssembledDraftExceedsCommentLengthLimit() throws Exception {
        when(ticketReplyDraftAiProvider.generateReplyDraft(any())).thenReturn(new TicketReplyDraftProviderResult(
                "Quick update from ops.",
                "x".repeat(1950),
                "Confirm whether the replacement restored printer health.",
                "I will add another internal update once the verification result is confirmed.",
                "gpt-4.1-mini",
                150,
                80,
                230,
                null
        ));
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/tickets/302/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-ai-reply-draft-too-long-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("ticket ai reply draft is unavailable"));

        assertThat(jdbcTemplate.queryForObject("SELECT interaction_type FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("REPLY_DRAFT");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("INVALID_RESPONSE");
        assertThat(jdbcTemplate.queryForObject("SELECT output_summary FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isNull();
    }

    private void assertNoTicketWorkflowMutation() {
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ticket WHERE id = ?", String.class, 302L)).isEqualTo("IN_PROGRESS");
        assertThat(jdbcTemplate.queryForObject("SELECT assignee_id FROM ticket WHERE id = ?", Long.class, 302L)).isEqualTo(102L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket_comment WHERE ticket_id = ?", Integer.class, 302L)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket_operation_log WHERE ticket_id = ?", Integer.class, 302L)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_event", Integer.class)).isEqualTo(0);
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
        jdbcTemplate.execute("CREATE TABLE audit_event (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, entity_type VARCHAR(64), entity_id BIGINT, action_type VARCHAR(64), created_at TIMESTAMP NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE ai_interaction_record (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, user_id BIGINT NOT NULL, request_id VARCHAR(128) NOT NULL, entity_type VARCHAR(64) NOT NULL, entity_id BIGINT NOT NULL, interaction_type VARCHAR(64) NOT NULL, prompt_version VARCHAR(128) NOT NULL, model_id VARCHAR(128), status VARCHAR(32) NOT NULL, latency_ms BIGINT NOT NULL, output_summary CLOB, usage_prompt_tokens INT, usage_completion_tokens INT, usage_total_tokens INT, usage_cost_micros BIGINT, created_at TIMESTAMP NOT NULL, CONSTRAINT fk_ai_interaction_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id), CONSTRAINT fk_ai_interaction_user_tenant FOREIGN KEY (user_id, tenant_id) REFERENCES users(id, tenant_id))");
    }

    private void seedTenants() {
        jdbcTemplate.update("INSERT INTO tenant (id, tenant_code, tenant_name, status, created_at, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", 1L, "demo-shop", "Demo Shop", "ACTIVE");
        jdbcTemplate.update("INSERT INTO tenant (id, tenant_code, tenant_name, status, created_at, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", 2L, "other-shop", "Other Shop", "ACTIVE");
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
        insertTicket(401L, 2L, "Other tenant ticket", "Other tenant issue", "OPEN", null, 201L, "seed-ticket-401");
    }

    private void seedComments() {
        jdbcTemplate.update("INSERT INTO ticket_comment (id, tenant_id, ticket_id, content, created_by, request_id, created_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)", 3101L, 1L, 302L, "Cable swap started.", 102L, "seed-ticket-comment-302");
    }

    private void seedOperationLogs() {
        jdbcTemplate.update("INSERT INTO ticket_operation_log (id, tenant_id, ticket_id, operation_type, detail, operator_id, request_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)", 3201L, 1L, 302L, "CREATED", "ticket created", 101L, "seed-ticket-log-created");
        jdbcTemplate.update("INSERT INTO ticket_operation_log (id, tenant_id, ticket_id, operation_type, detail, operator_id, request_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)", 3202L, 1L, 302L, "ASSIGNED", "assigned to ops", 101L, "seed-ticket-log-assigned");
        jdbcTemplate.update("INSERT INTO ticket_operation_log (id, tenant_id, ticket_id, operation_type, detail, operator_id, request_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)", 3203L, 1L, 302L, "STATUS_CHANGED", "status changed from OPEN to IN_PROGRESS", 102L, "seed-ticket-log-status");
    }

    private void insertPermission(Long id, String permissionCode, String permissionName) {
        jdbcTemplate.update("INSERT INTO permission (id, permission_code, permission_name, created_at, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", id, permissionCode, permissionName);
    }

    private void insertRole(Long id, Long tenantId, String roleCode, String roleName) {
        jdbcTemplate.update("INSERT INTO `role` (id, tenant_id, role_code, role_name, created_at, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", id, tenantId, roleCode, roleName);
    }

    private void insertUser(Long id,
                            Long tenantId,
                            String username,
                            String passwordHash,
                            String displayName,
                            String email,
                            String status) {
        jdbcTemplate.update("INSERT INTO users (id, tenant_id, username, password_hash, display_name, email, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", id, tenantId, username, passwordHash, displayName, email, status);
    }

    private void insertUserRole(Long id, Long userId, Long roleId) {
        jdbcTemplate.update("INSERT INTO user_role (id, user_id, role_id) VALUES (?, ?, ?)", id, userId, roleId);
    }

    private void insertRolePermission(Long id, Long roleId, Long permissionId) {
        jdbcTemplate.update("INSERT INTO role_permission (id, role_id, permission_id) VALUES (?, ?, ?)", id, roleId, permissionId);
    }

    private void insertTicket(Long id,
                              Long tenantId,
                              String title,
                              String description,
                              String status,
                              Long assigneeId,
                              Long createdBy,
                              String requestId) {
        jdbcTemplate.update("INSERT INTO ticket (id, tenant_id, title, description, status, assignee_id, created_by, request_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", id, tenantId, title, description, status, assigneeId, createdBy, requestId);
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
