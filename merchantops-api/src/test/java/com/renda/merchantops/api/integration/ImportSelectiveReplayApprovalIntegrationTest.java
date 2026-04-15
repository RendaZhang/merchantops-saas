package com.renda.merchantops.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.MerchantOpsApplication;
import com.renda.merchantops.api.support.TestAuthSessionSchemaSupport;
import com.renda.merchantops.api.importjob.messaging.ImportJobPublisher;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = MerchantOpsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:importselectivereplayapproval;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
                "merchantops.import.storage.local-dir=target/test-import-selective-replay-approval"
        }
)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ImportSelectiveReplayApprovalIntegrationTest {

    private static final Path STORAGE_ROOT = Path.of("target/test-import-selective-replay-approval");

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
    void setUp() throws Exception {
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
                CREATE TABLE approval_request (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    action_type VARCHAR(64) NOT NULL,
                    entity_type VARCHAR(64) NOT NULL,
                    entity_id BIGINT NOT NULL,
                    requested_by BIGINT NOT NULL,
                    reviewed_by BIGINT,
                    status VARCHAR(32) NOT NULL,
                    payload_json CLOB NOT NULL,
                    pending_request_key VARCHAR(191),
                    request_id VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    reviewed_at TIMESTAMP,
                    executed_at TIMESTAMP,
                    CONSTRAINT fk_approval_request_requested_by_tenant FOREIGN KEY (requested_by, tenant_id) REFERENCES users(id, tenant_id),
                    CONSTRAINT fk_approval_request_reviewed_by_tenant FOREIGN KEY (reviewed_by, tenant_id) REFERENCES users(id, tenant_id)
                )
                """);
        jdbcTemplate.execute("CREATE UNIQUE INDEX uk_approval_request_pending_request_key ON approval_request (pending_request_key)");
        jdbcTemplate.execute("""
                CREATE TABLE feature_flag (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    flag_key VARCHAR(128) NOT NULL,
                    enabled BOOLEAN NOT NULL,
                    updated_by BIGINT,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL,
                    CONSTRAINT uk_feature_flag_tenant_key UNIQUE (tenant_id, flag_key)
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
        jdbcTemplate.execute("""
                CREATE TABLE ai_interaction_record (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    entity_type VARCHAR(64) NOT NULL,
                    entity_id BIGINT NOT NULL,
                    interaction_type VARCHAR(64) NOT NULL,
                    prompt_version VARCHAR(128) NOT NULL,
                    model_id VARCHAR(128),
                    status VARCHAR(32) NOT NULL,
                    latency_ms BIGINT NOT NULL,
                    output_summary CLOB,
                    usage_prompt_tokens INT,
                    usage_completion_tokens INT,
                    usage_total_tokens INT,
                    usage_cost_micros BIGINT,
                    created_at TIMESTAMP NOT NULL,
                    CONSTRAINT fk_ai_interaction_user_tenant FOREIGN KEY (user_id, tenant_id) REFERENCES users(id, tenant_id)
                )
                """);

        TestAuthSessionSchemaSupport.createAuthSessionTable(jdbcTemplate);

        seedCoreData();
        seedImportReplayableSource();
        seedAiInteractions();
        seedFeatureFlags();

        if (Files.exists(STORAGE_ROOT)) {
            try (var walk = Files.walk(STORAGE_ROOT)) {
                walk.sorted((left, right) -> right.getNameCount() - left.getNameCount()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                });
            }
        }
        Files.createDirectories(STORAGE_ROOT.resolve("1"));
        Files.writeString(
                STORAGE_ROOT.resolve("1/source-proposal.csv"),
                """
                        username,displayName,email,password,roleCodes
                        retry-role,Retry Role,retry-role@example.com,abc123,RETRY_ROLE
                        retry-email,Retry Email,bad-email,abc123,READ_ONLY
                        """,
                StandardCharsets.UTF_8
        );
    }

    @Test
    void proposalCreateShouldRequireUserWriteAndHideCrossTenantSourceJobs() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");
        String outsiderToken = loginAndGetToken("other-shop", "outsider", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/replay-failures/selective/proposals")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header("X-Request-Id", "req-proposal-read-denied")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"errorCodes":["UNKNOWN_ROLE"]}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("permission denied"));

        mockMvc.perform(post("/api/v1/import-jobs/9999/replay-failures/selective/proposals")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header("X-Request-Id", "req-proposal-missing-job")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"errorCodes":["UNKNOWN_ROLE"]}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("import job not found"));

        mockMvc.perform(post("/api/v1/import-jobs/7001/replay-failures/selective/proposals")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(outsiderToken))
                        .header("X-Request-Id", "req-proposal-cross-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"errorCodes":["UNKNOWN_ROLE"]}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("import job not found"));
    }

    @Test
    void proposalCreateShouldReturnServiceUnavailableWhenBridgeFlagDisabled() throws Exception {
        setFeatureFlag("workflow.import.selective-replay-proposal.enabled", false);
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/replay-failures/selective/proposals")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header("X-Request-Id", "req-proposal-flag-disabled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"errorCodes":["UNKNOWN_ROLE"],"sourceInteractionId":9103}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("import selective replay proposal is disabled"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM approval_request", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_event", Integer.class)).isZero();
    }

    @Test
    void proposalCreateShouldPersistSafeApprovalPayloadAndAuditSnapshot() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");

        MvcResult result = mockMvc.perform(post("/api/v1/import-jobs/7001/replay-failures/selective/proposals")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header("X-Request-Id", "req-proposal-create-safe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "errorCodes": [" UNKNOWN_ROLE ", "UNKNOWN_ROLE"],
                                  "sourceInteractionId": 9103,
                                  "proposalReason": "  Review role fixes before replay  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actionType").value("IMPORT_JOB_SELECTIVE_REPLAY"))
                .andExpect(jsonPath("$.data.entityType").value("IMPORT_JOB"))
                .andExpect(jsonPath("$.data.entityId").value(7001))
                .andReturn();

        Long approvalRequestId = responseId(result);
        String payloadJson = jdbcTemplate.queryForObject(
                "SELECT payload_json FROM approval_request WHERE id = ?",
                String.class,
                approvalRequestId
        );
        assertThat(payloadJson).isEqualTo(
                "{\"sourceJobId\":7001,\"errorCodes\":[\"UNKNOWN_ROLE\"],\"sourceInteractionId\":9103,\"proposalReason\":\"Review role fixes before replay\"}"
        );
        assertThat(payloadJson)
                .doesNotContain("retry-role@example.com")
                .doesNotContain("retry-role,Retry Role,retry-role@example.com,abc123,RETRY_ROLE")
                .doesNotContain("abc123");

        String approvalAuditSnapshot = jdbcTemplate.queryForObject(
                "SELECT after_value FROM audit_event WHERE entity_type = 'APPROVAL_REQUEST' AND entity_id = ? AND action_type = 'APPROVAL_REQUEST_CREATED'",
                String.class,
                approvalRequestId
        );
        assertThat(approvalAuditSnapshot)
                .contains("IMPORT_JOB_SELECTIVE_REPLAY")
                .contains("\"payloadJson\":\"{\\\"sourceJobId\\\":7001,\\\"errorCodes\\\":[\\\"UNKNOWN_ROLE\\\"],\\\"sourceInteractionId\\\":9103,\\\"proposalReason\\\":\\\"Review role fixes before replay\\\"}\"")
                .doesNotContain("retry-role@example.com")
                .doesNotContain("retry-email")
                .doesNotContain("abc123");
    }

    @Test
    void proposalCreateShouldRejectDuplicatePendingProposalForSameCanonicalErrorCodes() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");

        Long approvalRequestId = createProposal(adminToken, "req-proposal-duplicate-first", """
                {
                  "errorCodes": ["UNKNOWN_ROLE", "INVALID_EMAIL"],
                  "sourceInteractionId": 9103,
                  "proposalReason": "first"
                }
                """);

        mockMvc.perform(post("/api/v1/import-jobs/7001/replay-failures/selective/proposals")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header("X-Request-Id", "req-proposal-duplicate-second")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "errorCodes": [" INVALID_EMAIL ", "UNKNOWN_ROLE", "UNKNOWN_ROLE"],
                                  "proposalReason": "second"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("pending selective replay proposal already exists for source job and selected errorCodes"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT payload_json FROM approval_request WHERE id = ?",
                String.class,
                approvalRequestId
        )).isEqualTo(
                "{\"sourceJobId\":7001,\"errorCodes\":[\"INVALID_EMAIL\",\"UNKNOWN_ROLE\"],\"sourceInteractionId\":9103,\"proposalReason\":\"first\"}"
        );
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM approval_request WHERE action_type = 'IMPORT_JOB_SELECTIVE_REPLAY'",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM approval_request WHERE action_type = 'IMPORT_JOB_SELECTIVE_REPLAY' AND status = 'PENDING'",
                Integer.class
        )).isEqualTo(1);
    }

    @Test
    void proposalCreateShouldRejectNonReplayableErrorCodesAndInvalidSourceInteraction() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");

        mockMvc.perform(post("/api/v1/import-jobs/7001/replay-failures/selective/proposals")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header("X-Request-Id", "req-proposal-invalid-error-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"errorCodes":["INVALID_HEADER"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("import job has no replayable failed rows for selected errorCodes"));

        mockMvc.perform(post("/api/v1/import-jobs/7001/replay-failures/selective/proposals")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header("X-Request-Id", "req-proposal-invalid-interaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "errorCodes": ["UNKNOWN_ROLE"],
                                  "sourceInteractionId": 9104
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("sourceInteractionId must reference a succeeded FIX_RECOMMENDATION interaction for the source import job"));
    }

    @Test
    void proposalCreateShouldAllowSamePayloadAgainAfterRejectingResolvedProposal() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");
        String reviewerToken = loginAndGetToken("demo-shop", "approver", "123456");

        Long firstApprovalRequestId = createProposal(adminToken, "req-proposal-retry-after-reject-first", """
                {
                  "errorCodes": ["UNKNOWN_ROLE"],
                  "sourceInteractionId": 9103
                }
                """);

        mockMvc.perform(post("/api/v1/approval-requests/{id}/reject", firstApprovalRequestId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reviewerToken))
                        .header("X-Request-Id", "req-proposal-retry-after-reject-review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        Long secondApprovalRequestId = createProposal(adminToken, "req-proposal-retry-after-reject-second", """
                {
                  "errorCodes": [" UNKNOWN_ROLE "]
                }
                """);

        assertThat(secondApprovalRequestId).isNotEqualTo(firstApprovalRequestId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pending_request_key FROM approval_request WHERE id = ?",
                String.class,
                firstApprovalRequestId
        )).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM approval_request WHERE id = ?",
                String.class,
                secondApprovalRequestId
        )).isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM approval_request WHERE action_type = 'IMPORT_JOB_SELECTIVE_REPLAY'",
                Integer.class
        )).isEqualTo(2);
    }

    @Test
    void proposalCreateShouldAllowSamePayloadAgainAfterApprovingResolvedProposal() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");
        String reviewerToken = loginAndGetToken("demo-shop", "approver", "123456");

        Long firstApprovalRequestId = createProposal(adminToken, "req-proposal-retry-after-approve-first", """
                {
                  "errorCodes": ["UNKNOWN_ROLE"]
                }
                """);

        mockMvc.perform(post("/api/v1/approval-requests/{id}/approve", firstApprovalRequestId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reviewerToken))
                        .header("X-Request-Id", "req-proposal-retry-after-approve-review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        Long secondApprovalRequestId = createProposal(adminToken, "req-proposal-retry-after-approve-second", """
                {
                  "errorCodes": ["UNKNOWN_ROLE"],
                  "proposalReason": "new review cycle"
                }
                """);

        assertThat(secondApprovalRequestId).isNotEqualTo(firstApprovalRequestId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pending_request_key FROM approval_request WHERE id = ?",
                String.class,
                firstApprovalRequestId
        )).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM approval_request WHERE id = ?",
                String.class,
                secondApprovalRequestId
        )).isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM import_job WHERE tenant_id = 1 AND source_job_id = 7001 AND request_id = 'req-proposal-retry-after-approve-review'",
                Integer.class
        )).isEqualTo(1);
    }

    @Test
    void approveShouldKeepSelfApprovalGuardAndCreateExactlyOneSelectiveReplayJob() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");
        String reviewerToken = loginAndGetToken("demo-shop", "approver", "123456");

        Long approvalRequestId = createProposal(adminToken, "req-proposal-self-guard-create", """
                {
                  "errorCodes": ["UNKNOWN_ROLE"],
                  "sourceInteractionId": 9103
                }
                """);

        mockMvc.perform(post("/api/v1/approval-requests/{id}/approve", approvalRequestId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header("X-Request-Id", "req-proposal-self-approve"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("requester cannot approve or reject own request"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM approval_request WHERE id = ?",
                String.class,
                approvalRequestId
        )).isEqualTo("PENDING");

        mockMvc.perform(post("/api/v1/approval-requests/{id}/approve", approvalRequestId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reviewerToken))
                        .header("X-Request-Id", "req-proposal-review-approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        Integer derivedJobCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM import_job WHERE tenant_id = 1 AND source_job_id = 7001 AND request_id = 'req-proposal-review-approve'",
                Integer.class
        );
        assertThat(derivedJobCount).isEqualTo(1);

        String storageKey = jdbcTemplate.queryForObject(
                "SELECT storage_key FROM import_job WHERE tenant_id = 1 AND source_job_id = 7001 AND request_id = 'req-proposal-review-approve'",
                String.class
        );
        String replayCsv = Files.readString(STORAGE_ROOT.resolve(storageKey), StandardCharsets.UTF_8);
        assertThat(replayCsv)
                .contains("username,displayName,email,password,roleCodes")
                .contains("retry-role,Retry Role,retry-role@example.com,abc123,RETRY_ROLE")
                .doesNotContain("retry-email,Retry Email,bad-email,abc123,READ_ONLY");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT reviewed_by FROM approval_request WHERE id = ?",
                Long.class,
                approvalRequestId
        )).isEqualTo(105L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM approval_request WHERE id = ?",
                String.class,
                approvalRequestId
        )).isEqualTo("APPROVED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE tenant_id = 1 AND entity_type = 'APPROVAL_REQUEST' AND entity_id = ? AND action_type = 'APPROVAL_ACTION_EXECUTED'",
                Integer.class,
                approvalRequestId
        )).isEqualTo(1);
    }

    @Test
    void reviewEndpointsShouldRejectAlreadyResolvedSelectiveReplayProposal() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");
        String reviewerToken = loginAndGetToken("demo-shop", "approver", "123456");

        Long approvedRequestId = createProposal(adminToken, "req-proposal-repeat-review-approved-create", """
                {
                  "errorCodes": ["UNKNOWN_ROLE"]
                }
                """);
        mockMvc.perform(post("/api/v1/approval-requests/{id}/approve", approvedRequestId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reviewerToken))
                        .header("X-Request-Id", "req-proposal-repeat-review-approved-final"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
        mockMvc.perform(post("/api/v1/approval-requests/{id}/approve", approvedRequestId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reviewerToken))
                        .header("X-Request-Id", "req-proposal-repeat-review-approved-repeat"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("approval request is not pending"));

        Long rejectedRequestId = createProposal(adminToken, "req-proposal-repeat-review-rejected-create", """
                {
                  "errorCodes": ["INVALID_EMAIL"]
                }
                """);
        mockMvc.perform(post("/api/v1/approval-requests/{id}/reject", rejectedRequestId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reviewerToken))
                        .header("X-Request-Id", "req-proposal-repeat-review-rejected-final"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
        mockMvc.perform(post("/api/v1/approval-requests/{id}/reject", rejectedRequestId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reviewerToken))
                        .header("X-Request-Id", "req-proposal-repeat-review-rejected-repeat"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("approval request is not pending"));
    }

    @Test
    void rejectShouldNotCreateReplayJob() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");
        String reviewerToken = loginAndGetToken("demo-shop", "approver", "123456");

        Long approvalRequestId = createProposal(adminToken, "req-proposal-reject-create", """
                {
                  "errorCodes": ["UNKNOWN_ROLE"]
                }
                """);

        mockMvc.perform(post("/api/v1/approval-requests/{id}/reject", approvalRequestId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(reviewerToken))
                        .header("X-Request-Id", "req-proposal-review-reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM approval_request WHERE id = ?",
                String.class,
                approvalRequestId
        )).isEqualTo("REJECTED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM import_job WHERE tenant_id = 1 AND source_job_id = 7001 AND request_id = 'req-proposal-review-reject'",
                Integer.class
        )).isZero();
    }

    @Test
    void proposalCreateShouldKeepOnlyOnePendingRowUnderConcurrentDuplicateRequests() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");
        String requestBody = """
                {
                  "errorCodes": ["UNKNOWN_ROLE"],
                  "sourceInteractionId": 9103
                }
                """;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<MvcResult>> futures = List.of(
                    executor.submit(() -> submitConcurrentProposal(adminToken, "req-proposal-concurrent-1", requestBody, ready, start)),
                    executor.submit(() -> submitConcurrentProposal(adminToken, "req-proposal-concurrent-2", requestBody, ready, start))
            );
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<MvcResult> results = new ArrayList<>();
            for (Future<MvcResult> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }

            assertThat(results.stream().map(result -> result.getResponse().getStatus()).toList())
                    .containsExactlyInAnyOrder(200, 400);
            assertThat(results.stream()
                    .filter(result -> result.getResponse().getStatus() == 400)
                    .map(this::responseMessage)
                    .toList()).containsExactly("pending selective replay proposal already exists for source job and selected errorCodes");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM approval_request WHERE action_type = 'IMPORT_JOB_SELECTIVE_REPLAY'",
                    Integer.class
            )).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM approval_request WHERE action_type = 'IMPORT_JOB_SELECTIVE_REPLAY' AND status = 'PENDING'",
                    Integer.class
            )).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private void seedCoreData() {
        String encodedPassword = passwordEncoder.encode("123456");

        jdbcTemplate.update("""
                INSERT INTO tenant (id, tenant_code, tenant_name, status, created_at, updated_at)
                VALUES (1, 'demo-shop', 'Demo Shop', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO tenant (id, tenant_code, tenant_name, status, created_at, updated_at)
                VALUES (2, 'other-shop', 'Other Shop', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        insertUser(101L, 1L, "admin", encodedPassword, "Demo Admin", "admin@demo-shop.local");
        insertUser(103L, 1L, "viewer", encodedPassword, "Viewer User", "viewer@demo-shop.local");
        insertUser(105L, 1L, "approver", encodedPassword, "Approval Reviewer", "approver@demo-shop.local");
        insertUser(201L, 2L, "outsider", encodedPassword, "Outsider User", "outsider@other-shop.local");

        insertRole(11L, 1L, "TENANT_ADMIN", "Tenant Admin");
        insertRole(13L, 1L, "READ_ONLY", "Read Only");
        insertRole(21L, 2L, "TENANT_ADMIN", "Tenant Admin");

        insertPermission(1L, "USER_READ", "Read users");
        insertPermission(2L, "USER_WRITE", "Write users");

        jdbcTemplate.update("INSERT INTO user_role (id, user_id, role_id) VALUES (1001, 101, 11)");
        jdbcTemplate.update("INSERT INTO user_role (id, user_id, role_id) VALUES (1003, 103, 13)");
        jdbcTemplate.update("INSERT INTO user_role (id, user_id, role_id) VALUES (1005, 105, 11)");
        jdbcTemplate.update("INSERT INTO user_role (id, user_id, role_id) VALUES (2001, 201, 21)");

        insertRolePermission(3001L, 11L, 1L);
        insertRolePermission(3002L, 11L, 2L);
        insertRolePermission(3003L, 13L, 1L);
        insertRolePermission(3004L, 21L, 1L);
        insertRolePermission(3005L, 21L, 2L);
    }

    private void seedImportReplayableSource() {
        jdbcTemplate.update("""
                INSERT INTO import_job (
                    id, tenant_id, import_type, source_type, source_filename, storage_key, source_job_id, status,
                    requested_by, request_id, total_count, success_count, failure_count, error_summary, created_at, started_at, finished_at
                )
                VALUES (
                    7001, 1, 'USER_CSV', 'CSV', 'source-proposal.csv', '1/source-proposal.csv', NULL, 'FAILED',
                    101, 'req-source-proposal', 2, 0, 2, 'all rows failed validation', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
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
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at)
                VALUES (7103, 1, 7001, NULL, 'INVALID_HEADER', 'header mismatch',
                        'username,displayName,email,password,roleCodes', CURRENT_TIMESTAMP)
                """);
    }

    private void seedAiInteractions() {
        jdbcTemplate.update("""
                INSERT INTO ai_interaction_record (
                    id, tenant_id, user_id, request_id, entity_type, entity_id, interaction_type, prompt_version, model_id,
                    status, latency_ms, output_summary, usage_prompt_tokens, usage_completion_tokens, usage_total_tokens, usage_cost_micros, created_at
                )
                VALUES (
                    9103, 1, 101, 'req-import-fix-recommendation-1', 'IMPORT_JOB', 7001, 'FIX_RECOMMENDATION',
                    'import-fix-recommendation-v1', 'gpt-4.1-mini', 'SUCCEEDED', 512,
                    'grouped UNKNOWN_ROLE as the highest-volume replay candidate', 120, 40, 160, 1200, CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO ai_interaction_record (
                    id, tenant_id, user_id, request_id, entity_type, entity_id, interaction_type, prompt_version, model_id,
                    status, latency_ms, output_summary, usage_prompt_tokens, usage_completion_tokens, usage_total_tokens, usage_cost_micros, created_at
                )
                VALUES (
                    9104, 1, 101, 'req-import-error-summary-1', 'IMPORT_JOB', 7001, 'ERROR_SUMMARY',
                    'import-error-summary-v1', 'gpt-4.1-mini', 'SUCCEEDED', 480,
                    'same import job but wrong interaction type for proposal provenance', 118, 36, 154, 1100, CURRENT_TIMESTAMP
                )
                """);
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

    private void insertUser(Long id,
                            Long tenantId,
                            String username,
                            String passwordHash,
                            String displayName,
                            String email) {
        jdbcTemplate.update("""
                INSERT INTO users (id, tenant_id, username, password_hash, display_name, email, status, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?)
                """, id, tenantId, username, passwordHash, displayName, email, id, id);
    }

    private void insertRole(Long id, Long tenantId, String roleCode, String roleName) {
        jdbcTemplate.update("""
                INSERT INTO `role` (id, tenant_id, role_code, role_name, created_at, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, id, tenantId, roleCode, roleName);
    }

    private void insertPermission(Long id, String permissionCode, String permissionName) {
        jdbcTemplate.update("""
                INSERT INTO permission (id, permission_code, permission_name, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, id, permissionCode, permissionName);
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
        jdbcTemplate.update("""
                UPDATE feature_flag
                SET enabled = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP
                WHERE tenant_id = ? AND flag_key = ?
                """, enabled, tenantId == 1L ? 101L : 201L, tenantId, key);
    }

    private void insertRolePermission(Long id, Long roleId, Long permissionId) {
        jdbcTemplate.update("""
                INSERT INTO role_permission (id, role_id, permission_id)
                VALUES (?, ?, ?)
                """, id, roleId, permissionId);
    }

    private Long createProposal(String token, String requestId, String requestBody) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/import-jobs/7001/replay-failures/selective/proposals")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .header("X-Request-Id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        return responseId(result);
    }

    private MvcResult submitConcurrentProposal(String token,
                                               String requestId,
                                               String requestBody,
                                               CountDownLatch ready,
                                               CountDownLatch start) throws Exception {
        ready.countDown();
        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
        return mockMvc.perform(post("/api/v1/import-jobs/7001/replay-failures/selective/proposals")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .header("X-Request-Id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
    }

    private Long responseId(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return root.path("data").path("id").asLong();
    }

    private String responseMessage(MvcResult result) {
        try {
            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
            return root.path("message").asText();
        } catch (Exception ex) {
            throw new IllegalStateException("failed to parse response message", ex);
        }
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
