package com.renda.merchantops.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.MerchantOpsApplication;
import com.renda.merchantops.api.support.TestAuthSessionSchemaSupport;
import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.core.AiProviderFailureType;
import com.renda.merchantops.api.ai.importjob.fixrecommendation.ImportJobFixRecommendationAiProvider;
import com.renda.merchantops.api.ai.importjob.fixrecommendation.ImportJobFixRecommendationProviderRequest;
import com.renda.merchantops.api.ai.importjob.fixrecommendation.ImportJobFixRecommendationProviderResult;
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
                "spring.datasource.url=jdbc:h2:mem:importaifixrecommendation;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
                "merchantops.ai.import-fix-recommendation-prompt-version=import-fix-recommendation-v1",
                "merchantops.ai.model-id=gpt-4.1-mini",
                "merchantops.ai.timeout-ms=5000",
                "merchantops.ai.base-url=https://api.openai.com",
                "merchantops.ai.api-key=test-openai-key"
        }
)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ImportJobAiFixRecommendationIntegrationTest {

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
    private ImportJobFixRecommendationAiProvider importJobFixRecommendationAiProvider;

    @BeforeEach
    void setUpSchemaAndData() {
        reset(importJobFixRecommendationAiProvider);
        aiProperties.setEnabled(true);
        aiProperties.setProvider(com.renda.merchantops.api.config.AiProviderType.OPENAI);
        aiProperties.setImportFixRecommendationPromptVersion("import-fix-recommendation-v1");
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
    void aiFixRecommendationShouldReturnRecommendationForAuthorizedViewerAndWriteInteractionRecordWithoutBusinessSideEffects() throws Exception {
        when(importJobFixRecommendationAiProvider.generateFixRecommendation(any())).thenReturn(successProviderResult());
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/ai-fix-recommendation")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-fix-recommendation-req-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.importJobId").value(7001))
                .andExpect(jsonPath("$.data.promptVersion").value("import-fix-recommendation-v1"))
                .andExpect(jsonPath("$.data.modelId").value("gpt-4.1-mini"))
                .andExpect(jsonPath("$.data.requestId").value("import-ai-fix-recommendation-req-1"))
                .andExpect(jsonPath("$.data.summary").value(successProviderResult().summary()))
                .andExpect(jsonPath("$.data.recommendedFixes[0].errorCode").value("UNKNOWN_ROLE"))
                .andExpect(jsonPath("$.data.recommendedFixes[0].affectedRowsEstimate").value(7))
                .andExpect(jsonPath("$.data.recommendedFixes[1].errorCode").value("DUPLICATE_USERNAME"))
                .andExpect(jsonPath("$.data.recommendedFixes[1].affectedRowsEstimate").value(2))
                .andExpect(jsonPath("$.data.confidenceNotes[0]").value("The recommendations are grounded in row-level error groups, so operators should still confirm tenant-specific business rules before reuse."))
                .andExpect(jsonPath("$.data.recommendedOperatorChecks[0]").value("Confirm which error-code group is the highest-volume cleanup target before editing replay input."))
                .andExpect(jsonPath("$.data.latencyMs").isNumber());

        ArgumentCaptor<ImportJobFixRecommendationProviderRequest> requestCaptor =
                ArgumentCaptor.forClass(ImportJobFixRecommendationProviderRequest.class);
        verify(importJobFixRecommendationAiProvider).generateFixRecommendation(requestCaptor.capture());
        String prompt = requestCaptor.getValue().prompt().userPrompt();
        assertThat(prompt).contains("UNKNOWN_ROLE");
        assertThat(prompt).contains("DUPLICATE_USERNAME");
        assertThat(prompt).contains("affectedRowsEstimate: 7");
        assertThat(prompt).contains("passwordPresent=true");
        assertThat(prompt).doesNotContain("retry-user@example.com");
        assertThat(prompt).doesNotContain("secret-pass-1");
        assertThat(prompt).doesNotContain("READ_ONLY");
        assertThat(prompt).doesNotContain("retry-user,Retry User,retry-user@example.com,secret-pass-1,READ_ONLY");

        assertNoImportBusinessSideEffects();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_interaction_record", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT entity_type FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isEqualTo("IMPORT_JOB");
        assertThat(jdbcTemplate.queryForObject("SELECT interaction_type FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isEqualTo("FIX_RECOMMENDATION");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isEqualTo("SUCCEEDED");
        assertThat(jdbcTemplate.queryForObject("SELECT output_summary FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L))
                .isEqualTo(successProviderResult().summary());
        assertThat(jdbcTemplate.queryForObject("SELECT usage_total_tokens FROM ai_interaction_record WHERE entity_id = ?", Integer.class, 7001L)).isEqualTo(232);
    }

    @Test
    void aiFixRecommendationShouldReturnForbiddenWhenUserReadPermissionMissing() throws Exception {
        jdbcTemplate.update("DELETE FROM role_permission WHERE role_id = ? AND permission_id = ?", 13L, 1L);
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/ai-fix-recommendation")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-fix-recommendation-no-read-1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("permission denied"));
    }

    @Test
    void aiFixRecommendationShouldReturnNotFoundForCrossTenantJob() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/8001/ai-fix-recommendation")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-fix-recommendation-cross-tenant-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("import job not found"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_interaction_record", Integer.class)).isEqualTo(0);
        assertNoImportBusinessSideEffects();
    }

    @Test
    void aiFixRecommendationShouldReturnBadRequestWhenJobHasNoFailureSignals() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7003/ai-fix-recommendation")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-fix-recommendation-no-failure-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("import job has no failure signals for fix recommendation"));

        verifyNoInteractions(importJobFixRecommendationAiProvider);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_interaction_record", Integer.class)).isEqualTo(0);
        assertNoImportBusinessSideEffects();
    }

    @Test
    void aiFixRecommendationShouldReturnBadRequestForUnsupportedImportType() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7005/ai-fix-recommendation")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-fix-recommendation-unsupported-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("import ai fix recommendation only supports USER_CSV"));

        verifyNoInteractions(importJobFixRecommendationAiProvider);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_interaction_record", Integer.class)).isEqualTo(0);
        assertNoImportBusinessSideEffects();
    }

    @Test
    void aiFixRecommendationShouldReturnBadRequestWhenJobHasNoRowSignal() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7004/ai-fix-recommendation")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-fix-recommendation-no-row-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("import job has no sanitized row signal for fix recommendation"));

        verifyNoInteractions(importJobFixRecommendationAiProvider);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_interaction_record", Integer.class)).isEqualTo(0);
        assertNoImportBusinessSideEffects();
    }

    @Test
    void aiFixRecommendationShouldReturnServiceUnavailableWhenProviderIsNotConfigured() throws Exception {
        aiProperties.setModelId(null);
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/ai-fix-recommendation")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-fix-recommendation-not-configured-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("import ai fix recommendation is unavailable"));

        verifyNoInteractions(importJobFixRecommendationAiProvider);
        assertNoImportBusinessSideEffects();
        assertThat(jdbcTemplate.queryForObject("SELECT interaction_type FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isEqualTo("FIX_RECOMMENDATION");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isEqualTo("PROVIDER_NOT_CONFIGURED");
    }

    @Test
    void aiFixRecommendationShouldReturnServiceUnavailableWhenProviderTimesOut() throws Exception {
        when(importJobFixRecommendationAiProvider.generateFixRecommendation(any()))
                .thenThrow(new AiProviderException(AiProviderFailureType.TIMEOUT, "timeout"));
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/ai-fix-recommendation")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-fix-recommendation-timeout-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("import ai fix recommendation timed out"));

        assertNoImportBusinessSideEffects();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isEqualTo("PROVIDER_TIMEOUT");
        assertThat(jdbcTemplate.queryForObject("SELECT output_summary FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isNull();
    }

    @Test
    void aiFixRecommendationShouldReturnServiceUnavailableWhenProviderIsUnavailable() throws Exception {
        when(importJobFixRecommendationAiProvider.generateFixRecommendation(any()))
                .thenThrow(new AiProviderException(AiProviderFailureType.UNAVAILABLE, "unavailable"));
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/ai-fix-recommendation")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-fix-recommendation-provider-unavailable-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("import ai fix recommendation is unavailable"));

        assertNoImportBusinessSideEffects();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isEqualTo("PROVIDER_UNAVAILABLE");
        assertThat(jdbcTemplate.queryForObject("SELECT output_summary FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isNull();
    }

    @Test
    void aiFixRecommendationShouldPersistInvalidResponseWhenProviderEchoesSensitiveValue() throws Exception {
        when(importJobFixRecommendationAiProvider.generateFixRecommendation(any()))
                .thenReturn(invalidProviderResult());
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/ai-fix-recommendation")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-fix-recommendation-invalid-response-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("import ai fix recommendation is unavailable"));

        assertNoImportBusinessSideEffects();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isEqualTo("INVALID_RESPONSE");
        assertThat(jdbcTemplate.queryForObject("SELECT output_summary FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isNull();
    }

    @Test
    void aiFixRecommendationShouldReturnServiceUnavailableWhenPersistedFixRecommendationFlagDisabled() throws Exception {
        setFeatureFlag("ai.import.fix-recommendation.enabled", false);
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/ai-fix-recommendation")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-fix-recommendation-flag-disabled-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("import ai fix recommendation is disabled"));

        verifyNoInteractions(importJobFixRecommendationAiProvider);
        assertNoImportBusinessSideEffects();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L))
                .isEqualTo("FEATURE_DISABLED");
    }

    @Test
    void aiFixRecommendationShouldReturnServiceUnavailableWhenFeatureDisabled() throws Exception {
        aiProperties.setEnabled(false);
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/ai-fix-recommendation")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-fix-recommendation-disabled-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("import ai fix recommendation is disabled"));

        verifyNoInteractions(importJobFixRecommendationAiProvider);
        assertNoImportBusinessSideEffects();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ai_interaction_record WHERE entity_id = ?", String.class, 7001L)).isEqualTo("FEATURE_DISABLED");
    }

    private ImportJobFixRecommendationProviderResult successProviderResult() {
        return new ImportJobFixRecommendationProviderResult(
                "The job is mostly blocked by tenant role validation, with a smaller duplicate-username tail that should be handled as a separate cleanup step before replay.",
                java.util.List.of(
                        new ImportJobFixRecommendationProviderResult.RecommendedFix(
                                "UNKNOWN_ROLE",
                                "Verify that the referenced role codes exist in the current tenant and normalize the source role-code format before preparing replay input.",
                                "The grouped failures point to tenant role validation rather than CSV shape corruption.",
                                true
                        ),
                        new ImportJobFixRecommendationProviderResult.RecommendedFix(
                                "DUPLICATE_USERNAME",
                                "Review the source usernames against current-tenant users and prepare unique replacements outside the AI response before replay.",
                                "The grouped failures indicate a uniqueness conflict that needs an operator-reviewed edit.",
                                true
                        )
                ),
                java.util.List.of("The recommendations are grounded in row-level error groups, so operators should still confirm tenant-specific business rules before reuse."),
                java.util.List.of(
                        "Confirm which error-code group is the highest-volume cleanup target before editing replay input.",
                        "Review the affected rows in /errors so value changes can be prepared outside the AI response."
                ),
                "gpt-4.1-mini",
                148,
                84,
                232,
                null
        );
    }

    private ImportJobFixRecommendationProviderResult invalidProviderResult() {
        return new ImportJobFixRecommendationProviderResult(
                "The job is still blocked by role validation.",
                java.util.List.of(
                        new ImportJobFixRecommendationProviderResult.RecommendedFix(
                                "UNKNOWN_ROLE",
                                "Replace the invalid role with READ_ONLY before replay.",
                                "The grouped failures still reference READ_ONLY directly.",
                                true
                        )
                ),
                java.util.List.of("Operator review is still required."),
                java.util.List.of("Review the affected rows in /errors."),
                "gpt-4.1-mini",
                148,
                84,
                232,
                null
        );
    }

    private void assertNoImportBusinessSideEffects() {
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM import_job WHERE id = ?", String.class, 7001L)).isEqualTo("FAILED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM import_job WHERE id = ?", String.class, 7003L)).isEqualTo("SUCCEEDED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM import_job WHERE id = ?", String.class, 7004L)).isEqualTo("FAILED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM import_job WHERE id = ?", String.class, 7005L)).isEqualTo("FAILED");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM import_job_item_error WHERE import_job_id = ?", Integer.class, 7001L)).isEqualTo(9);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM import_job_item_error WHERE import_job_id = ?", Integer.class, 7004L)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM import_job_item_error WHERE import_job_id = ?", Integer.class, 7005L)).isEqualTo(1);
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
                    7001, 1, 'USER_CSV', 'CSV', 'row-errors.csv', '1/row-errors.csv', NULL, 'FAILED',
                    101, 'req-row-errors', 9, 0, 9, 'role validation and duplicate usernames blocked the import', TIMESTAMP '2026-03-28 09:00:00', TIMESTAMP '2026-03-28 09:00:02', TIMESTAMP '2026-03-28 09:00:05'
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job (
                    id, tenant_id, import_type, source_type, source_filename, storage_key, source_job_id, status,
                    requested_by, request_id, total_count, success_count, failure_count, error_summary, created_at, started_at, finished_at
                )
                VALUES (
                    7003, 1, 'USER_CSV', 'CSV', 'clean.csv', '1/clean.csv', NULL, 'SUCCEEDED',
                    101, 'req-clean', 2, 2, 0, NULL, TIMESTAMP '2026-03-28 09:10:00', TIMESTAMP '2026-03-28 09:10:02', TIMESTAMP '2026-03-28 09:10:05'
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job (
                    id, tenant_id, import_type, source_type, source_filename, storage_key, source_job_id, status,
                    requested_by, request_id, total_count, success_count, failure_count, error_summary, created_at, started_at, finished_at
                )
                VALUES (
                    7004, 1, 'USER_CSV', 'CSV', 'header-only.csv', '1/header-only.csv', NULL, 'FAILED',
                    101, 'req-header-only', 1, 0, 1, 'header validation failed before row processing', TIMESTAMP '2026-03-28 09:20:00', TIMESTAMP '2026-03-28 09:20:02', TIMESTAMP '2026-03-28 09:20:05'
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job (
                    id, tenant_id, import_type, source_type, source_filename, storage_key, source_job_id, status,
                    requested_by, request_id, total_count, success_count, failure_count, error_summary, created_at, started_at, finished_at
                )
                VALUES (
                    7005, 1, 'ORDER_CSV', 'CSV', 'orders.csv', '1/orders.csv', NULL, 'FAILED',
                    101, 'req-orders', 1, 0, 1, 'unsupported import type for fix recommendation', TIMESTAMP '2026-03-28 09:30:00', TIMESTAMP '2026-03-28 09:30:02', TIMESTAMP '2026-03-28 09:30:05'
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job (
                    id, tenant_id, import_type, source_type, source_filename, storage_key, source_job_id, status,
                    requested_by, request_id, total_count, success_count, failure_count, error_summary, created_at, started_at, finished_at
                )
                VALUES (
                    8001, 2, 'USER_CSV', 'CSV', 'other-tenant.csv', '2/other-tenant.csv', NULL, 'FAILED',
                    201, 'req-other-tenant', 1, 0, 1, 'other tenant failure', TIMESTAMP '2026-03-28 09:40:00', TIMESTAMP '2026-03-28 09:40:02', TIMESTAMP '2026-03-28 09:40:05'
                )
                """);

        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7101, 1, 7001, 2, 'UNKNOWN_ROLE', 'roleCodes must exist in current tenant',
                        'retry-user,Retry User,retry-user@example.com,secret-pass-1,READ_ONLY', TIMESTAMP '2026-03-28 09:00:04')
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7102, 1, 7001, 3, 'DUPLICATE_USERNAME', 'username already exists in current tenant',
                        'existing-user,Existing User,existing@example.com,password-2,READ_ONLY', TIMESTAMP '2026-03-28 09:00:05')
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7103, 1, 7001, 4, 'UNKNOWN_ROLE', 'roleCodes must exist in current tenant',
                        'retry-user-2,Retry User 2,retry-user-2@example.com,secret-pass-2,READ_ONLY', TIMESTAMP '2026-03-28 09:00:06')
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7104, 1, 7001, 5, 'UNKNOWN_ROLE', 'roleCodes must exist in current tenant',
                        'retry-user-3,Retry User 3,retry-user-3@example.com,secret-pass-3,READ_ONLY', TIMESTAMP '2026-03-28 09:00:07')
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7105, 1, 7001, 6, 'UNKNOWN_ROLE', 'roleCodes must exist in current tenant',
                        'retry-user-4,Retry User 4,retry-user-4@example.com,secret-pass-4,READ_ONLY', TIMESTAMP '2026-03-28 09:00:08')
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7106, 1, 7001, 7, 'UNKNOWN_ROLE', 'roleCodes must exist in current tenant',
                        'retry-user-5,Retry User 5,retry-user-5@example.com,secret-pass-5,READ_ONLY', TIMESTAMP '2026-03-28 09:00:09')
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7107, 1, 7001, 8, 'UNKNOWN_ROLE', 'roleCodes must exist in current tenant',
                        'retry-user-6,Retry User 6,retry-user-6@example.com,secret-pass-6,READ_ONLY', TIMESTAMP '2026-03-28 09:00:10')
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7108, 1, 7001, 9, 'UNKNOWN_ROLE', 'roleCodes must exist in current tenant',
                        'retry-user-7,Retry User 7,retry-user-7@example.com,secret-pass-7,READ_ONLY', TIMESTAMP '2026-03-28 09:00:11')
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7109, 1, 7001, 10, 'DUPLICATE_USERNAME', 'username already exists in current tenant',
                        'existing-user-2,Existing User 2,existing-2@example.com,password-3,READ_ONLY', TIMESTAMP '2026-03-28 09:00:12')
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7201, 1, 7004, NULL, 'INVALID_HEADER', 'header columns do not match USER_CSV',
                        'login,display_name,email_address,passwd,roles', TIMESTAMP '2026-03-28 09:20:04')
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7301, 1, 7005, 2, 'ORDER_STATE_INVALID', 'order state is invalid for import',
                        'order-1,INVALID', TIMESTAMP '2026-03-28 09:30:04')
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (8101, 2, 8001, 2, 'UNKNOWN_ROLE', 'roleCodes must exist in current tenant',
                        'other-user,Other User,other-user@example.com,secret-pass-2,READ_ONLY', TIMESTAMP '2026-03-28 09:40:04')
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
