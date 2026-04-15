package com.renda.merchantops.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.MerchantOpsApplication;
import com.renda.merchantops.api.support.TestAuthSessionSchemaSupport;
import com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryAiProvider;
import com.renda.merchantops.api.ai.importjob.errorsummary.ImportJobErrorSummaryProviderResult;
import com.renda.merchantops.api.ai.importjob.fixrecommendation.ImportJobFixRecommendationAiProvider;
import com.renda.merchantops.api.ai.importjob.fixrecommendation.ImportJobFixRecommendationProviderResult;
import com.renda.merchantops.api.ai.importjob.mappingsuggestion.ImportJobMappingSuggestionAiProvider;
import com.renda.merchantops.api.ai.importjob.mappingsuggestion.ImportJobMappingSuggestionProviderResult;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.filter.RequestIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = MerchantOpsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:importaiinteractionhistory;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
                "merchantops.ai.import-mapping-suggestion-prompt-version=import-mapping-suggestion-v1",
                "merchantops.ai.import-fix-recommendation-prompt-version=import-fix-recommendation-v1",
                "merchantops.ai.model-id=gpt-4.1-mini",
                "merchantops.ai.timeout-ms=5000",
                "merchantops.ai.base-url=https://api.openai.com",
                "merchantops.ai.api-key=test-openai-key"
        }
)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ImportJobAiInteractionHistoryIntegrationTest {

    private static final int SEEDED_AI_INTERACTION_COUNT = 6;

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

    @MockBean
    private ImportJobMappingSuggestionAiProvider importJobMappingSuggestionAiProvider;

    @MockBean
    private ImportJobFixRecommendationAiProvider importJobFixRecommendationAiProvider;

    @BeforeEach
    void setUpSchemaAndData() {
        reset(importJobErrorSummaryAiProvider, importJobMappingSuggestionAiProvider, importJobFixRecommendationAiProvider);
        aiProperties.setEnabled(true);
        aiProperties.setProvider(com.renda.merchantops.api.config.AiProviderType.OPENAI);
        aiProperties.setImportErrorSummaryPromptVersion("import-error-summary-v1");
        aiProperties.setImportMappingSuggestionPromptVersion("import-mapping-suggestion-v1");
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
        seedAiInteractions();
    }

    @Test
    void listAiInteractionsShouldReturnHistoryWithStableOrderingPaginationAndNoLeakage() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/import-jobs/7001/ai-interactions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("page", "0")
                        .queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(9103))
                .andExpect(jsonPath("$.data.items[0].interactionType").value("FIX_RECOMMENDATION"))
                .andExpect(jsonPath("$.data.items[0].status").value("INVALID_RESPONSE"))
                .andExpect(jsonPath("$.data.items[0].outputSummary").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].promptVersion").value("import-fix-recommendation-v1"))
                .andExpect(jsonPath("$.data.items[0].modelId").value("gpt-4.1-mini"))
                .andExpect(jsonPath("$.data.items[0].latencyMs").value(251))
                .andExpect(jsonPath("$.data.items[0].requestId").value("import-ai-fix-recommendation-invalid-response-1"))
                .andExpect(jsonPath("$.data.items[0].usagePromptTokens").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].usageCompletionTokens").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].usageTotalTokens").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].usageCostMicros").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].rawPrompt").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].rawProviderPayload").doesNotExist())
                .andExpect(jsonPath("$.data.items[1].id").value(9102))
                .andExpect(jsonPath("$.data.items[1].interactionType").value("MAPPING_SUGGESTION"))
                .andExpect(jsonPath("$.data.items[1].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.items[1].outputSummary").value("The failed header still looks close to the canonical USER_CSV schema, so the safest next step is to confirm the proposed field mappings before preparing any replay input."))
                .andExpect(jsonPath("$.data.items[1].requestId").value("import-ai-mapping-suggestion-req-1"))
                .andExpect(jsonPath("$.data.items[1].usagePromptTokens").value(141))
                .andExpect(jsonPath("$.data.items[1].usageCompletionTokens").value(71))
                .andExpect(jsonPath("$.data.items[1].usageTotalTokens").value(212))
                .andExpect(jsonPath("$.data.items[1].usageCostMicros").value(nullValue()));

        assertReadOnlyState(SEEDED_AI_INTERACTION_COUNT);
    }

    @Test
    void listAiInteractionsShouldReturnForbiddenWhenUserReadPermissionMissing() throws Exception {
        jdbcTemplate.update("DELETE FROM role_permission WHERE role_id = ? AND permission_id = ?", 13L, 1L);
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/import-jobs/7001/ai-interactions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("permission denied"));

        assertReadOnlyState(SEEDED_AI_INTERACTION_COUNT);
    }

    @Test
    void listAiInteractionsShouldReturnNotFoundForCrossTenantOrMissingJob() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/import-jobs/8001/ai-interactions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("import job not found"));

        mockMvc.perform(get("/api/v1/import-jobs/9999/ai-interactions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("import job not found"));

        assertReadOnlyState(SEEDED_AI_INTERACTION_COUNT);
    }

    @Test
    void listAiInteractionsShouldFilterByInteractionType() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/import-jobs/7001/ai-interactions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("interactionType", "MAPPING_SUGGESTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(9102))
                .andExpect(jsonPath("$.data.items[0].interactionType").value("MAPPING_SUGGESTION"))
                .andExpect(jsonPath("$.data.items[0].status").value("SUCCEEDED"));

        assertReadOnlyState(SEEDED_AI_INTERACTION_COUNT);
    }

    @Test
    void listAiInteractionsShouldFilterByStatus() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/import-jobs/7001/ai-interactions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("status", "INVALID_RESPONSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(9103))
                .andExpect(jsonPath("$.data.items[0].interactionType").value("FIX_RECOMMENDATION"))
                .andExpect(jsonPath("$.data.items[0].status").value("INVALID_RESPONSE"))
                .andExpect(jsonPath("$.data.items[0].usagePromptTokens").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].usageCompletionTokens").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].usageTotalTokens").value(nullValue()))
                .andExpect(jsonPath("$.data.items[0].usageCostMicros").value(nullValue()));

        assertReadOnlyState(SEEDED_AI_INTERACTION_COUNT);
    }

    @Test
    void listAiInteractionsShouldShowRowsWrittenByGenerationEndpoints() throws Exception {
        when(importJobErrorSummaryAiProvider.generateErrorSummary(any())).thenReturn(successErrorSummaryResult());
        when(importJobMappingSuggestionAiProvider.generateMappingSuggestion(any())).thenReturn(successMappingSuggestionResult());
        when(importJobFixRecommendationAiProvider.generateFixRecommendation(any())).thenReturn(successFixRecommendationResult());

        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/ai-error-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-error-summary-history-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestId").value("import-ai-error-summary-history-1"));

        mockMvc.perform(post("/api/v1/import-jobs/7001/ai-mapping-suggestion")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-mapping-suggestion-history-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestId").value("import-ai-mapping-suggestion-history-1"));

        mockMvc.perform(post("/api/v1/import-jobs/7001/ai-fix-recommendation")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "import-ai-fix-recommendation-history-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestId").value("import-ai-fix-recommendation-history-1"));

        MvcResult historyResult = mockMvc.perform(get("/api/v1/import-jobs/7001/ai-interactions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("page", "0")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.total").value(7))
                .andExpect(jsonPath("$.data.items[0].rawPrompt").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].rawProviderPayload").doesNotExist())
                .andReturn();

        assertGeneratedHistoryMatchesStoredOrder(historyResult);
        assertReadOnlyState(SEEDED_AI_INTERACTION_COUNT + 3);
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
        jdbcTemplate.execute("CREATE TABLE import_job (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, import_type VARCHAR(64) NOT NULL, source_type VARCHAR(32) NOT NULL, source_filename VARCHAR(255) NOT NULL, storage_key VARCHAR(512) NOT NULL, source_job_id BIGINT, status VARCHAR(32) NOT NULL, requested_by BIGINT NOT NULL, request_id VARCHAR(128) NOT NULL, total_count INT NOT NULL, success_count INT NOT NULL, failure_count INT NOT NULL, error_summary VARCHAR(512), created_at TIMESTAMP NOT NULL, started_at TIMESTAMP, finished_at TIMESTAMP)");
        jdbcTemplate.execute("CREATE TABLE import_job_item_error (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, import_job_id BIGINT NOT NULL, source_row_number INT, error_code VARCHAR(64) NOT NULL, error_message VARCHAR(512) NOT NULL, raw_payload CLOB, created_at TIMESTAMP NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE feature_flag (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, flag_key VARCHAR(128) NOT NULL, enabled BIT NOT NULL, updated_by BIGINT, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE ai_interaction_record (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, user_id BIGINT NOT NULL, request_id VARCHAR(128) NOT NULL, entity_type VARCHAR(64) NOT NULL, entity_id BIGINT NOT NULL, interaction_type VARCHAR(64) NOT NULL, prompt_version VARCHAR(128) NOT NULL, model_id VARCHAR(128), status VARCHAR(32) NOT NULL, latency_ms BIGINT NOT NULL, output_summary CLOB, usage_prompt_tokens INT, usage_completion_tokens INT, usage_total_tokens INT, usage_cost_micros BIGINT, created_at TIMESTAMP NOT NULL, CONSTRAINT fk_ai_interaction_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id), CONSTRAINT fk_ai_interaction_user_tenant FOREIGN KEY (user_id, tenant_id) REFERENCES users(id, tenant_id))");
        jdbcTemplate.execute("CREATE UNIQUE INDEX uk_feature_flag_tenant_flag_key ON feature_flag (tenant_id, flag_key)");
    }

    private void seedTenants() {
        jdbcTemplate.update("INSERT INTO tenant (id, tenant_code, tenant_name, status, created_at, updated_at) VALUES (1, 'demo-shop', 'Demo Shop', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO tenant (id, tenant_code, tenant_name, status, created_at, updated_at) VALUES (2, 'other-shop', 'Other Shop', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
    }

    private void seedPermissions() {
        jdbcTemplate.update("INSERT INTO permission (id, permission_code, permission_name, created_at, updated_at) VALUES (1, 'USER_READ', 'Read user', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
    }

    private void seedRoles() {
        jdbcTemplate.update("INSERT INTO `role` (id, tenant_id, role_code, role_name, created_at, updated_at) VALUES (11, 1, 'TENANT_ADMIN', 'Tenant Admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO `role` (id, tenant_id, role_code, role_name, created_at, updated_at) VALUES (13, 1, 'READ_ONLY', 'Read Only User', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO `role` (id, tenant_id, role_code, role_name, created_at, updated_at) VALUES (21, 2, 'TENANT_ADMIN', 'Tenant Admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
    }

    private void seedUsers() {
        String encodedPassword = passwordEncoder.encode("123456");
        jdbcTemplate.update("INSERT INTO users (id, tenant_id, username, password_hash, display_name, email, status, created_at, updated_at, created_by, updated_by) VALUES (101, 1, 'admin', ?, 'Demo Admin', 'admin@demo-shop.local', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 101, 101)", encodedPassword);
        jdbcTemplate.update("INSERT INTO users (id, tenant_id, username, password_hash, display_name, email, status, created_at, updated_at, created_by, updated_by) VALUES (103, 1, 'viewer', ?, 'Viewer User', 'viewer@demo-shop.local', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 103, 103)", encodedPassword);
        jdbcTemplate.update("INSERT INTO users (id, tenant_id, username, password_hash, display_name, email, status, created_at, updated_at, created_by, updated_by) VALUES (201, 2, 'outsider', ?, 'Other Tenant User', 'outsider@other-shop.local', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 201, 201)", encodedPassword);
    }

    private void seedUserRoles() {
        jdbcTemplate.update("INSERT INTO user_role (id, user_id, role_id) VALUES (1001, 101, 11)");
        jdbcTemplate.update("INSERT INTO user_role (id, user_id, role_id) VALUES (1002, 103, 13)");
        jdbcTemplate.update("INSERT INTO user_role (id, user_id, role_id) VALUES (1003, 201, 21)");
    }

    private void seedRolePermissions() {
        jdbcTemplate.update("INSERT INTO role_permission (id, role_id, permission_id) VALUES (2001, 11, 1)");
        jdbcTemplate.update("INSERT INTO role_permission (id, role_id, permission_id) VALUES (2002, 13, 1)");
        jdbcTemplate.update("INSERT INTO role_permission (id, role_id, permission_id) VALUES (2003, 21, 1)");
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
                "INSERT INTO feature_flag (id, tenant_id, flag_key, enabled, updated_by, created_at, updated_at) VALUES (?, ?, ?, ?, NULL, ?, ?)",
                id,
                tenantId,
                key,
                enabled,
                Timestamp.valueOf(LocalDateTime.of(2026, 4, 6, 13, 18)),
                Timestamp.valueOf(LocalDateTime.of(2026, 4, 6, 13, 18))
        );
    }

    private void seedImportJobs() {
        jdbcTemplate.update("""
                INSERT INTO import_job (
                    id, tenant_id, import_type, source_type, source_filename, storage_key, source_job_id, status,
                    requested_by, request_id, total_count, success_count, failure_count, error_summary, created_at, started_at, finished_at
                )
                VALUES (
                    7001, 1, 'USER_CSV', 'CSV', 'header-and-row-errors.csv', '1/header-and-row-errors.csv', NULL, 'FAILED',
                    101, 'req-header-and-row-errors', 3, 0, 3, 'header validation and row cleanup are both required',
                    TIMESTAMP '2026-03-28 09:00:00', TIMESTAMP '2026-03-28 09:00:02', TIMESTAMP '2026-03-28 09:00:05'
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job (
                    id, tenant_id, import_type, source_type, source_filename, storage_key, source_job_id, status,
                    requested_by, request_id, total_count, success_count, failure_count, error_summary, created_at, started_at, finished_at
                )
                VALUES (
                    7002, 1, 'USER_CSV', 'CSV', 'other-job.csv', '1/other-job.csv', NULL, 'FAILED',
                    101, 'req-other-job', 1, 0, 1, 'other tenant-one job',
                    TIMESTAMP '2026-03-28 09:10:00', TIMESTAMP '2026-03-28 09:10:02', TIMESTAMP '2026-03-28 09:10:05'
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job (
                    id, tenant_id, import_type, source_type, source_filename, storage_key, source_job_id, status,
                    requested_by, request_id, total_count, success_count, failure_count, error_summary, created_at, started_at, finished_at
                )
                VALUES (
                    8001, 2, 'USER_CSV', 'CSV', 'other-tenant.csv', '2/other-tenant.csv', NULL, 'FAILED',
                    201, 'req-other-tenant', 1, 0, 1, 'other tenant failure',
                    TIMESTAMP '2026-03-28 09:20:00', TIMESTAMP '2026-03-28 09:20:02', TIMESTAMP '2026-03-28 09:20:05'
                )
                """);

        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7101, 1, 7001, NULL, 'INVALID_HEADER', 'header columns do not match USER_CSV',
                        'login,display_name,email_address,passwd,roles', TIMESTAMP '2026-03-28 09:00:03')
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7102, 1, 7001, 2, 'UNKNOWN_ROLE', 'roleCodes must exist in current tenant',
                        'retry-user,Retry User,retry-user@example.com,secret-pass-1,RETRY_ROLE', TIMESTAMP '2026-03-28 09:00:04')
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7103, 1, 7001, 3, 'DUPLICATE_USERNAME', 'username already exists in current tenant',
                        'existing-user,Existing User,existing-user@example.com,password-2,READ_ONLY', TIMESTAMP '2026-03-28 09:00:05')
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7104, 1, 7001, 4, 'UNKNOWN_ROLE', 'roleCodes must exist in current tenant',
                        'retry-user-2,Retry User 2,retry-user-2@example.com,secret-pass-2,RETRY_ROLE', TIMESTAMP '2026-03-28 09:00:06')
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7201, 1, 7002, 2, 'UNKNOWN_ROLE', 'roleCodes must exist in current tenant',
                        'other-user,Other User,other-user@example.com,secret-pass-3,RETRY_ROLE', TIMESTAMP '2026-03-28 09:10:04')
                """);
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (8101, 2, 8001, 2, 'UNKNOWN_ROLE', 'roleCodes must exist in current tenant',
                        'outsider-user,Outsider User,outsider-user@example.com,secret-pass-4,RETRY_ROLE', TIMESTAMP '2026-03-28 09:20:04')
                """);
    }

    private void seedAiInteractions() {
        insertAiInteractionRecord(
                9101L, 1L, 103L, "import-ai-error-summary-req-1", "IMPORT_JOB", 7001L,
                "ERROR_SUMMARY", "import-error-summary-v1", "gpt-4.1-mini", "SUCCEEDED", 512L,
                "The job is dominated by tenant role validation failures, with structurally complete rows and no evidence of CSV shape corruption in the sanitized prompt window.",
                140, 72, 212, null, LocalDateTime.of(2026, 3, 28, 10, 40)
        );
        insertAiInteractionRecord(
                9102L, 1L, 103L, "import-ai-mapping-suggestion-req-1", "IMPORT_JOB", 7001L,
                "MAPPING_SUGGESTION", "import-mapping-suggestion-v1", "gpt-4.1-mini", "SUCCEEDED", 544L,
                "The failed header still looks close to the canonical USER_CSV schema, so the safest next step is to confirm the proposed field mappings before preparing any replay input.",
                141, 71, 212, null, LocalDateTime.of(2026, 3, 28, 10, 45)
        );
        insertAiInteractionRecord(
                9103L, 1L, 103L, "import-ai-fix-recommendation-invalid-response-1", "IMPORT_JOB", 7001L,
                "FIX_RECOMMENDATION", "import-fix-recommendation-v1", "gpt-4.1-mini", "INVALID_RESPONSE", 251L,
                null, null, null, null, null, LocalDateTime.of(2026, 3, 28, 10, 45)
        );
        insertAiInteractionRecord(
                9104L, 1L, 103L, "import-ai-error-summary-disabled-1", "IMPORT_JOB", 7001L,
                "ERROR_SUMMARY", "import-error-summary-v1", "gpt-4.1-mini", "FEATURE_DISABLED", 0L,
                null, null, null, null, null, LocalDateTime.of(2026, 3, 28, 10, 20)
        );
        insertAiInteractionRecord(
                9201L, 1L, 103L, "import-ai-other-job-1", "IMPORT_JOB", 7002L,
                "ERROR_SUMMARY", "import-error-summary-v1", "gpt-4.1-mini", "SUCCEEDED", 488L,
                "Other job summary.", 130, 60, 190, 2100L, LocalDateTime.of(2026, 3, 28, 10, 30)
        );
        insertAiInteractionRecord(
                9301L, 2L, 201L, "import-ai-other-tenant-1", "IMPORT_JOB", 8001L,
                "ERROR_SUMMARY", "import-error-summary-v1", "gpt-4.1-mini", "SUCCEEDED", 477L,
                "Other tenant summary.", 128, 58, 186, 2050L, LocalDateTime.of(2026, 3, 28, 10, 35)
        );
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

    private ImportJobErrorSummaryProviderResult successErrorSummaryResult() {
        return new ImportJobErrorSummaryProviderResult(
                "The job is dominated by tenant role validation failures, with structurally complete rows and no evidence of CSV shape corruption in the sanitized prompt window.",
                List.of(
                        "UNKNOWN_ROLE appears on multiple rows where roleCodes is present but invalid for the current tenant.",
                        "The prompt window rows are structurally complete, so the current failures look like business validation rather than malformed CSV input."
                ),
                List.of(
                        "Review the current tenant role catalog and correct invalid roleCodes before retrying.",
                        "Use import job detail and /errors to choose between edited replay and selective replay after the role fixes are confirmed."
                ),
                "gpt-4.1-mini",
                140,
                72,
                212,
                null
        );
    }

    private ImportJobMappingSuggestionProviderResult successMappingSuggestionResult() {
        return new ImportJobMappingSuggestionProviderResult(
                "The failed header still looks close to the canonical USER_CSV schema, so the safest next step is to confirm the proposed field mappings before preparing any replay input.",
                List.of(
                        new ImportJobMappingSuggestionProviderResult.SuggestedFieldMapping(
                                "username",
                                new ImportJobMappingSuggestionProviderResult.ObservedColumnSignal("login", 1),
                                "`login` is the closest observed header for the tenant username field.",
                                false
                        ),
                        new ImportJobMappingSuggestionProviderResult.SuggestedFieldMapping(
                                "displayName",
                                new ImportJobMappingSuggestionProviderResult.ObservedColumnSignal("display_name", 2),
                                "`display_name` is the closest semantic match for displayName.",
                                false
                        ),
                        new ImportJobMappingSuggestionProviderResult.SuggestedFieldMapping(
                                "email",
                                new ImportJobMappingSuggestionProviderResult.ObservedColumnSignal("email_address", 3),
                                "`email_address` is the most likely email column.",
                                false
                        ),
                        new ImportJobMappingSuggestionProviderResult.SuggestedFieldMapping(
                                "password",
                                new ImportJobMappingSuggestionProviderResult.ObservedColumnSignal("passwd", 4),
                                "`passwd` should be manually confirmed before replay.",
                                true
                        ),
                        new ImportJobMappingSuggestionProviderResult.SuggestedFieldMapping(
                                "roleCodes",
                                new ImportJobMappingSuggestionProviderResult.ObservedColumnSignal("roles", 5),
                                "`roles` is the closest available signal for roleCodes.",
                                true
                        )
                ),
                List.of("The source file failed header validation, so each mapping should be reviewed before downstream use."),
                List.of(
                        "Confirm the source header order before editing any replay input.",
                        "Verify that the observed `roles` column really contains tenant role codes in the expected delimiter format."
                ),
                "gpt-4.1-mini",
                141,
                71,
                212,
                null
        );
    }

    private ImportJobFixRecommendationProviderResult successFixRecommendationResult() {
        return new ImportJobFixRecommendationProviderResult(
                "The job is mostly blocked by tenant role validation, with a smaller duplicate-username tail that should be handled as a separate cleanup step before replay.",
                List.of(
                        new ImportJobFixRecommendationProviderResult.RecommendedFix(
                                "UNKNOWN_ROLE",
                                "Verify that the referenced role codes exist in the current tenant and normalize the source role-code format before preparing replay input.",
                                "The sampled failures point to tenant role validation rather than CSV shape corruption.",
                                true
                        ),
                        new ImportJobFixRecommendationProviderResult.RecommendedFix(
                                "DUPLICATE_USERNAME",
                                "Review the source usernames against current-tenant users and prepare unique replacements outside the AI response before replay.",
                                "The sampled failures indicate a uniqueness conflict that needs an operator-reviewed edit.",
                                true
                        )
                ),
                List.of("The recommendations are grounded in row-level error groups, so operators should still confirm tenant-specific business rules before reuse."),
                List.of(
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

    private void assertGeneratedHistoryMatchesStoredOrder(MvcResult historyResult) throws Exception {
        List<Long> expectedIds = jdbcTemplate.queryForList(
                """
                        SELECT id
                        FROM ai_interaction_record
                        WHERE tenant_id = ? AND entity_type = 'IMPORT_JOB' AND entity_id = ?
                        ORDER BY created_at DESC, id DESC
                        """,
                Long.class,
                1L,
                7001L
        );
        JsonNode root = objectMapper.readTree(historyResult.getResponse().getContentAsByteArray());
        JsonNode items = root.path("data").path("items");
        assertThat(items).hasSize(expectedIds.size());
        assertThat(java.util.stream.StreamSupport.stream(items.spliterator(), false)
                .map(node -> node.path("id").asLong())
                .toList()).isEqualTo(expectedIds);
        assertThat(java.util.stream.StreamSupport.stream(items.spliterator(), false)
                .map(node -> node.path("requestId").asText())
                .toList()).contains(
                "import-ai-error-summary-history-1",
                "import-ai-mapping-suggestion-history-1",
                "import-ai-fix-recommendation-history-1"
        );
        assertThat(java.util.stream.StreamSupport.stream(items.spliterator(), false)
                .map(node -> node.path("interactionType").asText())
                .toList()).contains("ERROR_SUMMARY", "MAPPING_SUGGESTION", "FIX_RECOMMENDATION");
    }

    private void assertReadOnlyState(int expectedAiInteractionCount) {
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_interaction_record", Integer.class))
                .isEqualTo(expectedAiInteractionCount);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM import_job", Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM import_job WHERE source_job_id IS NOT NULL", Integer.class))
                .isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM import_job WHERE id = ?", String.class, 7001L))
                .isEqualTo("FAILED");
        assertThat(jdbcTemplate.queryForObject("SELECT total_count FROM import_job WHERE id = ?", Integer.class, 7001L))
                .isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT success_count FROM import_job WHERE id = ?", Integer.class, 7001L))
                .isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject("SELECT failure_count FROM import_job WHERE id = ?", Integer.class, 7001L))
                .isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM import_job_item_error WHERE import_job_id = ?", Integer.class, 7001L))
                .isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_event", Integer.class)).isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM approval_request", Integer.class)).isEqualTo(0);
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
