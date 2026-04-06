package com.renda.merchantops.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.MerchantOpsApplication;
import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.core.AiProviderFailureType;
import com.renda.merchantops.api.ai.ticket.triage.TicketTriageAiProvider;
import com.renda.merchantops.api.ai.ticket.triage.TicketTriageProviderRequest;
import com.renda.merchantops.api.ai.ticket.triage.TicketTriageProviderResult;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.dto.ticket.query.TicketAiTriagePriority;
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
                "spring.datasource.url=jdbc:h2:mem:ticketailtriage;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
                "merchantops.ai.model-id=gpt-4.1-mini",
                "merchantops.ai.timeout-ms=5000",
                "merchantops.ai.base-url=https://api.openai.com",
                "merchantops.ai.api-key=test-openai-key"
        }
)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TicketAiTriageIntegrationTest {

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
    private TicketTriageAiProvider ticketTriageAiProvider;

    @BeforeEach
    void setUpSchemaAndData() {
        assertThat(currentJdbcUrl()).contains("jdbc:h2:mem:ticketailtriage");
        assertThat(currentDatabaseMode()).isEqualTo("MySQL");

        reset(ticketTriageAiProvider);
        aiProperties.setEnabled(true);
        aiProperties.setProvider(com.renda.merchantops.api.config.AiProviderType.OPENAI);
        aiProperties.setPromptVersion("ticket-summary-v1");
        aiProperties.setTriagePromptVersion("ticket-triage-v1");
        aiProperties.setModelId("gpt-4.1-mini");
        aiProperties.setTimeoutMs(5000);
        aiProperties.setApiKey("test-openai-key");
        aiProperties.setBaseUrl("https://api.openai.com");

        jdbcTemplate.execute("DROP ALL OBJECTS");
        createSchema();

        seedTenants();
        seedPermissions();
        seedRoles();
        seedUsers();
        seedUserRoles();
        seedRolePermissions();
        seedFeatureFlags();
        seedTickets();
        seedComments();
        seedOperationLogs();
    }

    @Test
    void aiTriageShouldReturnTriageForAuthorizedViewerAndWriteInteractionRecordWithoutBusinessSideEffects() throws Exception {
        when(ticketTriageAiProvider.generateTriage(any())).thenReturn(new TicketTriageProviderResult(
                "DEVICE_ISSUE",
                TicketAiTriagePriority.HIGH,
                "The ticket describes a store printer outage during active operations and the latest signal shows work is still in progress, so it should be treated as a high-priority device issue.",
                "gpt-4.1-mini",
                132,
                58,
                190,
                null
        ));
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/tickets/302/ai-triage")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-ai-triage-req-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.ticketId").value(302))
                .andExpect(jsonPath("$.data.classification").value("DEVICE_ISSUE"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.promptVersion").value("ticket-triage-v1"))
                .andExpect(jsonPath("$.data.modelId").value("gpt-4.1-mini"))
                .andExpect(jsonPath("$.data.requestId").value("ticket-ai-triage-req-1"))
                .andExpect(jsonPath("$.data.reasoning").value("The ticket describes a store printer outage during active operations and the latest signal shows work is still in progress, so it should be treated as a high-priority device issue."))
                .andExpect(jsonPath("$.data.latencyMs").isNumber());

        ArgumentCaptor<TicketTriageProviderRequest> requestCaptor = ArgumentCaptor.forClass(TicketTriageProviderRequest.class);
        verify(ticketTriageAiProvider).generateTriage(requestCaptor.capture());
        assertThat(requestCaptor.getValue().ticketId()).isEqualTo(302L);
        assertThat(requestCaptor.getValue().prompt().userPrompt()).contains("Cable swap started.");
        assertThat(requestCaptor.getValue().prompt().userPrompt()).contains("status changed from OPEN to IN_PROGRESS");

        assertNoTicketWorkflowMutation();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_interaction_record", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT tenant_id FROM ai_interaction_record WHERE entity_id = ?", Long.class, 302L)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT user_id FROM ai_interaction_record WHERE entity_id = ?", Long.class, 302L)).isEqualTo(103L);
        assertThat(jdbcTemplate.queryForObject("SELECT request_id FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("ticket-ai-triage-req-1");
        assertThat(jdbcTemplate.queryForObject("SELECT interaction_type FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("TRIAGE");
        assertThat(jdbcTemplate.queryForObject("SELECT prompt_version FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("ticket-triage-v1");
        assertThat(jdbcTemplate.queryForObject("SELECT model_id FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("gpt-4.1-mini");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("SUCCEEDED");
        assertThat(jdbcTemplate.queryForObject("SELECT output_summary FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("classification=DEVICE_ISSUE; priority=HIGH");
        assertThat(jdbcTemplate.queryForObject("SELECT usage_total_tokens FROM ai_interaction_record WHERE entity_id = ?", Integer.class, 302L)).isEqualTo(190);
    }

    @Test
    void aiTriageShouldReturnForbiddenWhenTicketReadPermissionMissing() throws Exception {
        jdbcTemplate.update("DELETE FROM role_permission WHERE role_id = ? AND permission_id = ?", 13L, 6L);
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/tickets/302/ai-triage")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-ai-triage-no-read-1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("permission denied"));
    }

    @Test
    void aiTriageShouldReturnNotFoundForCrossTenantTicket() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/tickets/401/ai-triage")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-ai-triage-cross-tenant-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("ticket not found"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_interaction_record", Integer.class)).isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_event", Integer.class)).isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM approval_request", Integer.class)).isEqualTo(0);
    }

    @Test
    void aiTriageShouldReturnServiceUnavailableWhenProviderIsNotConfigured() throws Exception {
        aiProperties.setModelId(null);
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/tickets/302/ai-triage")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-ai-triage-not-configured-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("ticket ai triage is unavailable"));

        verifyNoInteractions(ticketTriageAiProvider);
        assertNoTicketWorkflowMutation();
        assertThat(jdbcTemplate.queryForObject("SELECT interaction_type FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("TRIAGE");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("PROVIDER_NOT_CONFIGURED");
    }

    @Test
    void aiTriageShouldReturnServiceUnavailableWhenProviderTimesOut() throws Exception {
        when(ticketTriageAiProvider.generateTriage(any())).thenThrow(new AiProviderException(AiProviderFailureType.TIMEOUT, "timeout"));
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/tickets/302/ai-triage")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-ai-triage-timeout-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("ticket ai triage timed out"));

        assertNoTicketWorkflowMutation();
        assertThat(jdbcTemplate.queryForObject("SELECT interaction_type FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("TRIAGE");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("PROVIDER_TIMEOUT");
        assertThat(jdbcTemplate.queryForObject("SELECT output_summary FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isNull();
    }

    @Test
    void aiTriageShouldReturnServiceUnavailableWhenProviderIsUnavailable() throws Exception {
        when(ticketTriageAiProvider.generateTriage(any())).thenThrow(new AiProviderException(AiProviderFailureType.UNAVAILABLE, "unavailable"));
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/tickets/302/ai-triage")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-ai-triage-provider-unavailable-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("ticket ai triage is unavailable"));

        assertNoTicketWorkflowMutation();
        assertThat(jdbcTemplate.queryForObject("SELECT interaction_type FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("TRIAGE");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("PROVIDER_UNAVAILABLE");
        assertThat(jdbcTemplate.queryForObject("SELECT output_summary FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isNull();
    }

    @Test
    void aiTriageShouldPersistInvalidResponseWhenProviderResultViolatesOutputPolicy() throws Exception {
        when(ticketTriageAiProvider.generateTriage(any())).thenReturn(new TicketTriageProviderResult(
                "DEVICE_ISSUE",
                null,
                "The ticket describes a store printer outage during active operations.",
                "gpt-4.1-mini",
                132,
                58,
                190,
                null
        ));
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/tickets/302/ai-triage")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-ai-triage-invalid-response-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("ticket ai triage is unavailable"));

        assertNoTicketWorkflowMutation();
        assertThat(jdbcTemplate.queryForObject("SELECT interaction_type FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("TRIAGE");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("INVALID_RESPONSE");
        assertThat(jdbcTemplate.queryForObject("SELECT output_summary FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isNull();
    }

    @Test
    void aiTriageShouldReturnServiceUnavailableWhenPersistedTriageFlagDisabled() throws Exception {
        setFeatureFlag("ai.ticket.triage.enabled", false);
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/tickets/302/ai-triage")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-ai-triage-flag-disabled-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("ticket ai triage is disabled"));

        verifyNoInteractions(ticketTriageAiProvider);
        assertNoTicketWorkflowMutation();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L))
                .isEqualTo("FEATURE_DISABLED");
    }

    @Test
    void aiTriageShouldReturnServiceUnavailableWhenFeatureDisabled() throws Exception {
        aiProperties.setEnabled(false);
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/tickets/302/ai-triage")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-ai-triage-disabled-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("ticket ai triage is disabled"));

        verifyNoInteractions(ticketTriageAiProvider);
        assertNoTicketWorkflowMutation();
        assertThat(jdbcTemplate.queryForObject("SELECT interaction_type FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("TRIAGE");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 302L)).isEqualTo("FEATURE_DISABLED");
    }

    private void assertNoTicketWorkflowMutation() {
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ticket WHERE id = ?", String.class, 302L)).isEqualTo("IN_PROGRESS");
        assertThat(jdbcTemplate.queryForObject("SELECT assignee_id FROM ticket WHERE id = ?", Long.class, 302L)).isEqualTo(102L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket_comment WHERE ticket_id = ?", Integer.class, 302L)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket_operation_log WHERE ticket_id = ?", Integer.class, 302L)).isEqualTo(3);
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
        jdbcTemplate.execute("CREATE TABLE feature_flag (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, flag_key VARCHAR(128) NOT NULL, enabled BOOLEAN NOT NULL, updated_by BIGINT, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL, CONSTRAINT uk_feature_flag_tenant_key UNIQUE (tenant_id, flag_key))");
        jdbcTemplate.execute("CREATE TABLE ai_interaction_record (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, user_id BIGINT NOT NULL, request_id VARCHAR(128) NOT NULL, entity_type VARCHAR(64) NOT NULL, entity_id BIGINT NOT NULL, interaction_type VARCHAR(64) NOT NULL, prompt_version VARCHAR(128) NOT NULL, model_id VARCHAR(128), status VARCHAR(32) NOT NULL, latency_ms BIGINT NOT NULL, output_summary CLOB, usage_prompt_tokens INT, usage_completion_tokens INT, usage_total_tokens INT, usage_cost_micros BIGINT, created_at TIMESTAMP NOT NULL, CONSTRAINT fk_ai_interaction_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id), CONSTRAINT fk_ai_interaction_user_tenant FOREIGN KEY (user_id, tenant_id) REFERENCES users(id, tenant_id))");
    }

    private void seedFeatureFlags() {
        insertFeatureFlag(1L, "ai.ticket.summary.enabled", true);
        insertFeatureFlag(2L, "ai.ticket.triage.enabled", true);
        insertFeatureFlag(3L, "ai.ticket.reply-draft.enabled", true);
        insertFeatureFlag(4L, "ai.import.error-summary.enabled", true);
        insertFeatureFlag(5L, "ai.import.mapping-suggestion.enabled", true);
        insertFeatureFlag(6L, "ai.import.fix-recommendation.enabled", true);
        insertFeatureFlag(7L, "workflow.import.selective-replay-proposal.enabled", true);
        insertFeatureFlag(8L, "workflow.ticket.comment-proposal.enabled", true);
    }

    private void insertFeatureFlag(Long id, String key, boolean enabled) {
        insertFeatureFlag(1L, id, key, enabled);
    }

    private void insertFeatureFlag(Long tenantId, Long id, String key, boolean enabled) {
        jdbcTemplate.update(
                "INSERT INTO feature_flag (id, tenant_id, flag_key, enabled, updated_by, created_at, updated_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id,
                tenantId,
                key,
                enabled,
                tenantId == 1L ? 101L : 201L
        );
    }

    private void setFeatureFlag(String key, boolean enabled) {
        setFeatureFlag(1L, key, enabled);
    }

    private void setFeatureFlag(Long tenantId, String key, boolean enabled) {
        jdbcTemplate.update(
                "UPDATE feature_flag SET enabled = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND flag_key = ?",
                enabled,
                tenantId == 1L ? 101L : 201L,
                tenantId,
                key
        );
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
        insertTicket(401L, 2L, "Other tenant ticket", "Other tenant issue", "OPEN", null, 201L, "seed-ticket-401");
    }

    private void seedComments() {
        jdbcTemplate.update("""
                INSERT INTO ticket_comment (id, tenant_id, ticket_id, content, created_by, request_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, 3101L, 1L, 302L, "Cable swap started.", 102L, "seed-ticket-comment-302");
    }

    private void seedOperationLogs() {
        jdbcTemplate.update("""
                INSERT INTO ticket_operation_log (id, tenant_id, ticket_id, operation_type, detail, operator_id, request_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, 3201L, 1L, 302L, "CREATED", "ticket created", 101L, "seed-ticket-log-created");
        jdbcTemplate.update("""
                INSERT INTO ticket_operation_log (id, tenant_id, ticket_id, operation_type, detail, operator_id, request_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, 3202L, 1L, 302L, "ASSIGNED", "assigned to ops", 101L, "seed-ticket-log-assigned");
        jdbcTemplate.update("""
                INSERT INTO ticket_operation_log (id, tenant_id, ticket_id, operation_type, detail, operator_id, request_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, 3203L, 1L, 302L, "STATUS_CHANGED", "status changed from OPEN to IN_PROGRESS", 102L, "seed-ticket-log-status");
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
