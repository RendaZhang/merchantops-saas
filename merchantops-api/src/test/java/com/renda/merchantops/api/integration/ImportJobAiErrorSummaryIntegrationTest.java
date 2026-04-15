package com.renda.merchantops.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.MerchantOpsApplication;
import com.renda.merchantops.api.support.TestAuthSessionSchemaSupport;
import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.core.AiProviderFailureType;
import com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryAiProvider;
import com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryProviderRequest;
import com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryProviderResult;
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
                "spring.datasource.url=jdbc:h2:mem:importaierrorsummary;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
                "merchantops.ai.enabled=true",
                "merchantops.ai.provider=OPENAI",
                "merchantops.ai.import-error-summary-prompt-version=import-error-summary-v1",
                "merchantops.ai.model-id=gpt-4.1-mini",
                "merchantops.ai.timeout-ms=5000",
                "merchantops.ai.base-url=https://api.openai.com",
                "merchantops.ai.api-key=test-openai-key"
        }
)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ImportJobAiErrorSummaryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AiProperties aiProperties;

    @MockBean
    private ImportJobErrorSummaryAiProvider importJobErrorSummaryAiProvider;

    @BeforeEach
    void setUpSchemaAndData() {
        reset(importJobErrorSummaryAiProvider);
        aiProperties.setEnabled(true);
        aiProperties.setProvider(com.renda.merchantops.api.config.AiProviderType.OPENAI);
        aiProperties.setImportErrorSummaryPromptVersion("import-error-summary-v1");
        aiProperties.setModelId("gpt-4.1-mini");
        aiProperties.setTimeoutMs(5000);
        aiProperties.setApiKey("test-openai-key");
        aiProperties.setBaseUrl("https://api.openai.com");

        jdbcTemplate.execute("DROP ALL OBJECTS");
        createSchema();
        TestAuthSessionSchemaSupport.createAuthSessionTable(jdbcTemplate);

        seedTenants();
        seedPermissions();
        seedRoles();
        seedUsers();
        seedUserRoles();
        seedRolePermissions();
        seedFeatureFlags();
        seedImportJobs();
    }

    @Test
    void aiErrorSummaryShouldReturnSummaryForAuthorizedViewerAndWriteInteractionRecordWithoutBusinessSideEffects() throws Exception {
        when(importJobErrorSummaryAiProvider.generateErrorSummary(any())).thenReturn(new ImportJobErrorSummaryProviderResult(
                "The job is dominated by role validation failures, and the sanitized prompt window shows structurally complete rows rather than malformed CSV.",
                java.util.List.of(
                        "UNKNOWN_ROLE appears on multiple rows where roleCodes is present but invalid for the current tenant.",
                        "The first prompt-window rows are structurally complete, which points to business validation instead of CSV shape errors."
                ),
                java.util.List.of(
                        "Review tenant role mappings and correct invalid roleCodes before replaying failures.",
                        "Use detail and /errors to decide whether edited replay or selective replay is the safer retry path."
                ),
                "gpt-4.1-mini",
                140,
                72,
                212,
                null
        ));
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/ai-error-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-error-summary-req-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.importJobId").value(7001))
                .andExpect(jsonPath("$.data.promptVersion").value("import-error-summary-v1"))
                .andExpect(jsonPath("$.data.modelId").value("gpt-4.1-mini"))
                .andExpect(jsonPath("$.data.requestId").value("import-ai-error-summary-req-1"))
                .andExpect(jsonPath("$.data.summary").value("The job is dominated by role validation failures, and the sanitized prompt window shows structurally complete rows rather than malformed CSV."))
                .andExpect(jsonPath("$.data.topErrorPatterns[0]").value("UNKNOWN_ROLE appears on multiple rows where roleCodes is present but invalid for the current tenant."))
                .andExpect(jsonPath("$.data.recommendedNextSteps[0]").value("Review tenant role mappings and correct invalid roleCodes before replaying failures."))
                .andExpect(jsonPath("$.data.latencyMs").isNumber());

        ArgumentCaptor<ImportJobErrorSummaryProviderRequest> requestCaptor = ArgumentCaptor.forClass(ImportJobErrorSummaryProviderRequest.class);
        verify(importJobErrorSummaryAiProvider).generateErrorSummary(requestCaptor.capture());
        String prompt = requestCaptor.getValue().prompt().userPrompt();
        assertThat(prompt).contains("UNKNOWN_ROLE: 2");
        assertThat(prompt).contains("passwordPresent=true");
        assertThat(prompt).contains("roleCodeCount=1");
        assertThat(prompt).doesNotContain("retry-role");
        assertThat(prompt).doesNotContain("retry-role@example.com");
        assertThat(prompt).doesNotContain("abc123");
        assertThat(prompt).doesNotContain("retry-role,Retry Role,retry-role@example.com,abc123,RETRY_ROLE");

        assertNoImportBusinessSideEffects();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_interaction_record", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT entity_type FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isEqualTo("IMPORT_JOB");
        assertThat(jdbcTemplate.queryForObject("SELECT interaction_type FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isEqualTo("ERROR_SUMMARY");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isEqualTo("SUCCEEDED");
        assertThat(jdbcTemplate.queryForObject("SELECT output_summary FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L))
                .isEqualTo("The job is dominated by role validation failures, and the sanitized prompt window shows structurally complete rows rather than malformed CSV.");
        assertThat(jdbcTemplate.queryForObject("SELECT usage_total_tokens FROM ai_interaction_record WHERE entity_id = ?", Integer.class, 7001L)).isEqualTo(212);
    }

    @Test
    void aiErrorSummaryShouldReturnForbiddenWhenUserReadPermissionMissing() throws Exception {
        jdbcTemplate.update("DELETE FROM role_permission WHERE role_id = ? AND permission_id = ?", 13L, 1L);
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/ai-error-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-error-summary-no-read-1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("permission denied"));
    }

    @Test
    void aiErrorSummaryShouldReturnNotFoundForCrossTenantJob() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/8001/ai-error-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-error-summary-cross-tenant-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("import job not found"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_interaction_record", Integer.class)).isEqualTo(0);
        assertNoImportBusinessSideEffects();
    }

    @Test
    void aiErrorSummaryShouldReturnServiceUnavailableWhenProviderIsNotConfigured() throws Exception {
        aiProperties.setModelId(null);
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/ai-error-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-error-summary-not-configured-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("import ai error summary is unavailable"));

        verifyNoInteractions(importJobErrorSummaryAiProvider);
        assertNoImportBusinessSideEffects();
        assertThat(jdbcTemplate.queryForObject("SELECT interaction_type FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isEqualTo("ERROR_SUMMARY");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isEqualTo("PROVIDER_NOT_CONFIGURED");
    }

    @Test
    void aiErrorSummaryShouldReturnServiceUnavailableWhenProviderTimesOut() throws Exception {
        when(importJobErrorSummaryAiProvider.generateErrorSummary(any()))
                .thenThrow(new AiProviderException(AiProviderFailureType.TIMEOUT, "timeout"));
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/ai-error-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-error-summary-timeout-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("import ai error summary timed out"));

        assertNoImportBusinessSideEffects();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isEqualTo("PROVIDER_TIMEOUT");
        assertThat(jdbcTemplate.queryForObject("SELECT output_summary FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isNull();
    }

    @Test
    void aiErrorSummaryShouldReturnServiceUnavailableWhenProviderIsUnavailable() throws Exception {
        when(importJobErrorSummaryAiProvider.generateErrorSummary(any()))
                .thenThrow(new AiProviderException(AiProviderFailureType.UNAVAILABLE, "unavailable"));
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/ai-error-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-error-summary-provider-unavailable-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("import ai error summary is unavailable"));

        assertNoImportBusinessSideEffects();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isEqualTo("PROVIDER_UNAVAILABLE");
        assertThat(jdbcTemplate.queryForObject("SELECT output_summary FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isNull();
    }

    @Test
    void aiErrorSummaryShouldPersistInvalidResponseWhenProviderResultViolatesOutputPolicy() throws Exception {
        when(importJobErrorSummaryAiProvider.generateErrorSummary(any())).thenReturn(new ImportJobErrorSummaryProviderResult(
                "   ",
                java.util.List.of("pattern one"),
                java.util.List.of("step one"),
                "gpt-4.1-mini",
                140,
                72,
                212,
                null
        ));
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/ai-error-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-error-summary-invalid-response-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("import ai error summary is unavailable"));

        assertNoImportBusinessSideEffects();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isEqualTo("INVALID_RESPONSE");
        assertThat(jdbcTemplate.queryForObject("SELECT output_summary FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isNull();
    }

    @Test
    void aiErrorSummaryShouldReturnServiceUnavailableWhenPersistedErrorSummaryFlagDisabled() throws Exception {
        setFeatureFlag("ai.import.error-summary.enabled", false);
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/ai-error-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-error-summary-flag-disabled-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("import ai error summary is disabled"));

        verifyNoInteractions(importJobErrorSummaryAiProvider);
        assertNoImportBusinessSideEffects();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L))
                .isEqualTo("FEATURE_DISABLED");
    }

    @Test
    void aiErrorSummaryShouldReturnServiceUnavailableWhenFeatureDisabled() throws Exception {
        aiProperties.setEnabled(false);
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/ai-error-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-error-summary-disabled-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("import ai error summary is disabled"));

        verifyNoInteractions(importJobErrorSummaryAiProvider);
        assertNoImportBusinessSideEffects();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isEqualTo("FEATURE_DISABLED");
    }

    private void assertNoImportBusinessSideEffects() {
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM import_job WHERE id = ?", String.class, 7001L)).isEqualTo("FAILED");
        assertThat(jdbcTemplate.queryForObject("SELECT total_count FROM import_job WHERE id = ?", Integer.class, 7001L)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT success_count FROM import_job WHERE id = ?", Integer.class, 7001L)).isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject("SELECT failure_count FROM import_job WHERE id = ?", Integer.class, 7001L)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM import_job_item_error WHERE import_job_id = ?", Integer.class, 7001L)).isEqualTo(2);
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
        jdbcTemplate.execute("CREATE TABLE audit_event (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, entity_type VARCHAR(64) NOT NULL, entity_id BIGINT NOT NULL, action_type VARCHAR(64) NOT NULL, operator_id BIGINT NOT NULL, request_id VARCHAR(128) NOT NULL, before_value CLOB, after_value CLOB, approval_status VARCHAR(32) NOT NULL, created_at TIMESTAMP NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE approval_request (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, action_type VARCHAR(64) NOT NULL, entity_type VARCHAR(64) NOT NULL, entity_id BIGINT NOT NULL, requested_by BIGINT NOT NULL, reviewed_by BIGINT, status VARCHAR(32) NOT NULL, payload_json CLOB NOT NULL, pending_request_key VARCHAR(191), request_id VARCHAR(128) NOT NULL, created_at TIMESTAMP NOT NULL, reviewed_at TIMESTAMP, executed_at TIMESTAMP, CONSTRAINT fk_approval_request_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id), CONSTRAINT fk_approval_request_requested_by_tenant FOREIGN KEY (requested_by, tenant_id) REFERENCES users(id, tenant_id), CONSTRAINT fk_approval_request_reviewed_by_tenant FOREIGN KEY (reviewed_by, tenant_id) REFERENCES users(id, tenant_id))");
        jdbcTemplate.execute("CREATE UNIQUE INDEX uk_approval_request_pending_request_key ON approval_request (pending_request_key)");
        jdbcTemplate.execute("CREATE TABLE feature_flag (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, flag_key VARCHAR(128) NOT NULL, enabled BOOLEAN NOT NULL, updated_by BIGINT, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL, CONSTRAINT uk_feature_flag_tenant_key UNIQUE (tenant_id, flag_key))");
        jdbcTemplate.execute("CREATE TABLE import_job (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, import_type VARCHAR(64) NOT NULL, source_type VARCHAR(32) NOT NULL, source_filename VARCHAR(255) NOT NULL, storage_key VARCHAR(512) NOT NULL, source_job_id BIGINT, status VARCHAR(32) NOT NULL, requested_by BIGINT NOT NULL, request_id VARCHAR(128) NOT NULL, total_count INT NOT NULL, success_count INT NOT NULL, failure_count INT NOT NULL, error_summary VARCHAR(512), created_at TIMESTAMP NOT NULL, started_at TIMESTAMP, finished_at TIMESTAMP)");
        jdbcTemplate.execute("CREATE TABLE import_job_item_error (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, import_job_id BIGINT NOT NULL, source_row_number INT, error_code VARCHAR(64) NOT NULL, error_message VARCHAR(512) NOT NULL, raw_payload CLOB, created_at TIMESTAMP NOT NULL)");
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
        jdbcTemplate.update("INSERT INTO tenant (id, tenant_code, tenant_name, status, created_at, updated_at) VALUES (1, 'demo-shop', 'Demo Shop', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO tenant (id, tenant_code, tenant_name, status, created_at, updated_at) VALUES (2, 'other-shop', 'Other Shop', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
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

    private void seedImportJobs() {
        jdbcTemplate.update("""
                INSERT INTO import_job (
                    id, tenant_id, import_type, source_type, source_filename, storage_key, source_job_id, status,
                    requested_by, request_id, total_count, success_count, failure_count, error_summary, created_at, started_at, finished_at
                )
                VALUES (
                    7001, 1, 'USER_CSV', 'CSV', 'source-authz.csv', '1/source-authz.csv', NULL, 'FAILED',
                    101, 'req-source-authz', 2, 0, 2, 'all rows failed validation', TIMESTAMP '2026-03-27 09:00:00', TIMESTAMP '2026-03-27 09:00:02', TIMESTAMP '2026-03-27 09:00:05'
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job (
                    id, tenant_id, import_type, source_type, source_filename, storage_key, source_job_id, status,
                    requested_by, request_id, total_count, success_count, failure_count, error_summary, created_at, started_at, finished_at
                )
                VALUES (
                    8001, 2, 'USER_CSV', 'CSV', 'other-source.csv', '2/other-source.csv', NULL, 'FAILED',
                    201, 'req-other-source', 1, 0, 1, 'other tenant failure', TIMESTAMP '2026-03-27 09:10:00', TIMESTAMP '2026-03-27 09:10:02', TIMESTAMP '2026-03-27 09:10:05'
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7101, 1, 7001, 2, 'UNKNOWN_ROLE', 'roleCodes must exist in current tenant',
                        'retry-role,Retry Role,retry-role@example.com,abc123,RETRY_ROLE', TIMESTAMP '2026-03-27 09:00:04')
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7102, 1, 7001, 3, 'UNKNOWN_ROLE', 'roleCodes must exist in current tenant',
                        'retry-role-2,Retry Role 2,retry-role-2@example.com,abc123,RETRY_ROLE', TIMESTAMP '2026-03-27 09:00:04')
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (8101, 2, 8001, 2, 'UNKNOWN_ROLE', 'roleCodes must exist in current tenant',
                        'other-row,Other Row,other-row@example.com,abc123,RETRY_ROLE', TIMESTAMP '2026-03-27 09:10:04')
                """);
    }

    private void insertPermission(Long id, String permissionCode, String permissionName) {
        jdbcTemplate.update("INSERT INTO permission (id, permission_code, permission_name, created_at, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", id, permissionCode, permissionName);
    }

    private void insertRole(Long id, Long tenantId, String roleCode, String roleName) {
        jdbcTemplate.update("INSERT INTO `role` (id, tenant_id, role_code, role_name, created_at, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", id, tenantId, roleCode, roleName);
    }

    private void insertUser(Long id, Long tenantId, String username, String passwordHash, String displayName, String email, String status) {
        jdbcTemplate.update("INSERT INTO users (id, tenant_id, username, password_hash, display_name, email, status, created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?)",
                id, tenantId, username, passwordHash, displayName, email, status, id, id);
    }

    private void insertUserRole(Long id, Long userId, Long roleId) {
        jdbcTemplate.update("INSERT INTO user_role (id, user_id, role_id) VALUES (?, ?, ?)", id, userId, roleId);
    }

    private void insertRolePermission(Long id, Long roleId, Long permissionId) {
        jdbcTemplate.update("INSERT INTO role_permission (id, role_id, permission_id) VALUES (?, ?, ?)", id, roleId, permissionId);
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
}
