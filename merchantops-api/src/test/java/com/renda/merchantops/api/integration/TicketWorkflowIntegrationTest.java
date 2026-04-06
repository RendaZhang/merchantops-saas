package com.renda.merchantops.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.MerchantOpsApplication;
import com.renda.merchantops.api.filter.RequestIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = MerchantOpsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:ticketworkflow;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
class TicketWorkflowIntegrationTest {

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
        assertThat(currentJdbcUrl()).contains("jdbc:h2:mem:ticketworkflow");
        assertThat(currentDatabaseMode()).isEqualTo("MySQL");

        jdbcTemplate.execute("DROP ALL OBJECTS");

        jdbcTemplate.execute("""
                CREATE TABLE tenant (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    tenant_code VARCHAR(64) NOT NULL,
                    tenant_name VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    role_code VARCHAR(64) NOT NULL,
                    role_name VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE permission (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    permission_code VARCHAR(64) NOT NULL,
                    permission_name VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE user_role (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    role_id BIGINT NOT NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE role_permission (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
                    created_at TIMESTAMP NOT NULL,
                    CONSTRAINT fk_audit_event_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
                    CONSTRAINT fk_audit_event_operator_tenant FOREIGN KEY (operator_id, tenant_id) REFERENCES users(id, tenant_id)
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
                    CONSTRAINT fk_approval_request_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
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
                CREATE TABLE ticket (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    title VARCHAR(128) NOT NULL,
                    description VARCHAR(2000),
                    status VARCHAR(32) NOT NULL,
                    assignee_id BIGINT,
                    created_by BIGINT NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
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
                    CONSTRAINT fk_ai_interaction_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
                    CONSTRAINT fk_ai_interaction_user_tenant FOREIGN KEY (user_id, tenant_id) REFERENCES users(id, tenant_id)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE ticket_comment (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    ticket_id BIGINT NOT NULL,
                    content VARCHAR(2000) NOT NULL,
                    created_by BIGINT NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE ticket_operation_log (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    ticket_id BIGINT NOT NULL,
                    operation_type VARCHAR(64) NOT NULL,
                    detail VARCHAR(512) NOT NULL,
                    operator_id BIGINT NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);

        seedTenants();
        seedPermissions();
        seedRoles();
        seedUsers();
        seedUserRoles();
        seedRolePermissions();
        seedFeatureFlags();
        seedTickets();
        seedAiInteractions();
    }

    @Test
    void listTicketsShouldReturnTenantScopedPageForViewer() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("page", "0")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.items[*].id", contains(303, 302, 301)))
                .andExpect(jsonPath("$.data.items[*].title", not(hasItem("Other tenant ticket"))));
    }


    @Test
    void listTicketsShouldSupportStatusAndAssigneeFilters() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("status", "IN_PROGRESS")
                        .queryParam("assigneeId", "102"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[*].id", contains(302)))
                .andExpect(jsonPath("$.data.items[0].assigneeId").value(102));
    }

    @Test
    void listTicketsAssigneeFilterShouldNotLeakCrossTenantAssigneeId() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("assigneeId", "201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void listTicketsShouldSupportUnassignedOnlyAndStatusWithStablePaging() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("status", "OPEN")
                        .queryParam("unassignedOnly", "true")
                        .queryParam("page", "0")
                        .queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.items[*].id", contains(301)));
    }

    @Test
    void listTicketsKeywordShouldMatchTitleAndDescriptionWithinTenantOnly() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        Long titleKeywordTicketId = createTicketAndGetId(adminToken, "keyword-title-req-1", "TitleOnlyKeyword-ops", "normal description");
        Long descriptionKeywordTicketId = createTicketAndGetId(adminToken, "keyword-description-req-1", "normal title", "DescOnlyKeyword-router");

        mockMvc.perform(get("/api/v1/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("keyword", "other tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/v1/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("keyword", "TitleOnlyKeyword-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[*].id", contains(titleKeywordTicketId.intValue())));

        mockMvc.perform(get("/api/v1/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("keyword", "DescOnlyKeyword-router"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[*].id", contains(descriptionKeywordTicketId.intValue())));
    }

    @Test
    void listTicketsShouldRejectAssigneeWithUnassignedOnly() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("assigneeId", "102")
                        .queryParam("unassignedOnly", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void getTicketDetailShouldReturnNotFoundWhenTicketIsOutsideCurrentTenant() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/tickets/401")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("ticket not found"));
    }

    @Test
    void viewerShouldNotBeAllowedToCreateTicket() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTicketRequest("POS printer offline", "desc")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("permission denied"));
    }

    @Test
    void assignTicketShouldRejectAssigneeOutsideCurrentTenant() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");

        mockMvc.perform(patch("/api/v1/tickets/301/assignee")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "assign-outside-req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignTicketRequest(201L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("user must exist in current tenant"));
    }

    @Test
    void updateTicketStatusShouldAllowReopenFromClosedToOpen() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");

        String previousUpdatedAt = jdbcTemplate.queryForObject("SELECT CAST(updated_at AS VARCHAR) FROM ticket WHERE id = ?", String.class, 303L);

        mockMvc.perform(patch("/api/v1/tickets/303/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "reopen-req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusUpdateRequest("OPEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.assigneeId").value(102));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ticket WHERE id = ?", String.class, 303L))
                .isEqualTo("OPEN");

        String reopenedUpdatedAt = jdbcTemplate.queryForObject("SELECT CAST(updated_at AS VARCHAR) FROM ticket WHERE id = ?", String.class, 303L);
        assertThat(reopenedUpdatedAt).isNotEqualTo(previousUpdatedAt);

        assertThat(jdbcTemplate.queryForList("""
                        SELECT operation_type
                        FROM ticket_operation_log
                        WHERE ticket_id = ?
                        ORDER BY id
                        """, String.class, 303L))
                .contains("STATUS_CHANGED");
        assertThat(jdbcTemplate.queryForObject("""
                        SELECT detail
                        FROM ticket_operation_log
                        WHERE ticket_id = ?
                        ORDER BY id DESC
                        LIMIT 1
                        """, String.class, 303L))
                .isEqualTo("status changed from CLOSED to OPEN");

        mockMvc.perform(get("/api/v1/tickets/303")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    void updateTicketStatusShouldRejectNoopClosedToClosedTransition() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");

        mockMvc.perform(patch("/api/v1/tickets/303/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "closed-noop-req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusUpdateRequest("CLOSED")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("ticket is already in status CLOSED"));
    }

    @Test
    void ticketWorkflowShouldWriteOperationLogsForCreateAssignStatusCommentAndClose() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");
        String opsToken = loginAndGetToken("demo-shop", "ops", "123456");

        Long ticketId = createTicketAndGetId(adminToken, "create-req-1", "POS register frozen", "Register screen froze during checkout");

        mockMvc.perform(patch("/api/v1/tickets/" + ticketId + "/assignee")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "assign-req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignTicketRequest(102L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assigneeUsername").value("ops"));

        mockMvc.perform(patch("/api/v1/tickets/" + ticketId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(opsToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "status-req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusUpdateRequest("IN_PROGRESS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(opsToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "comment-req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentRequest("Investigating store terminal logs")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdByUsername").value("ops"));

        mockMvc.perform(patch("/api/v1/tickets/" + ticketId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(opsToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "close-req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusUpdateRequest("CLOSED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ticket WHERE id = ?", String.class, ticketId))
                .isEqualTo("CLOSED");
        assertThat(jdbcTemplate.queryForObject("SELECT assignee_id FROM ticket WHERE id = ?", Long.class, ticketId))
                .isEqualTo(102L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket_comment WHERE ticket_id = ?", Integer.class, ticketId))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForList("""
                        SELECT operation_type
                        FROM ticket_operation_log
                        WHERE ticket_id = ?
                        ORDER BY id
                        """, String.class, ticketId))
                .containsExactly("CREATED", "ASSIGNED", "STATUS_CHANGED", "COMMENTED", "STATUS_CHANGED");
        assertThat(jdbcTemplate.queryForList("""
                        SELECT request_id
                        FROM ticket_operation_log
                        WHERE ticket_id = ?
                        ORDER BY id
                        """, String.class, ticketId))
                .containsExactly("create-req-1", "assign-req-1", "status-req-1", "comment-req-1", "close-req-1");

        mockMvc.perform(get("/api/v1/tickets/" + ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.comments", hasSize(1)))
                .andExpect(jsonPath("$.data.operationLogs", hasSize(5)))
                .andExpect(jsonPath("$.data.operationLogs[*].operationType",
                        contains("CREATED", "ASSIGNED", "STATUS_CHANGED", "COMMENTED", "STATUS_CHANGED")));
    }

    @Test
    void createCommentProposalShouldPersistSafePayloadAndApprovalAuditSnapshot() throws Exception {
        String opsToken = loginAndGetToken("demo-shop", "ops", "123456");

        MvcResult result = mockMvc.perform(post("/api/v1/tickets/301/comments/proposals/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(opsToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-create-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentProposalRequest("  Draft reply for the store. Cable swap verified.  ", 9002L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actionType").value("TICKET_COMMENT_CREATE"))
                .andExpect(jsonPath("$.data.entityType").value("TICKET"))
                .andExpect(jsonPath("$.data.entityId").value(301))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.requestedBy").value(102))
                .andReturn();

        Long approvalRequestId = objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .path("data").path("id").asLong();

        String payloadJson = jdbcTemplate.queryForObject(
                "SELECT payload_json FROM approval_request WHERE id = ?",
                String.class,
                approvalRequestId
        );
        assertThat(payloadJson).isEqualTo(
                "{\"commentContent\":\"Draft reply for the store. Cable swap verified.\",\"sourceInteractionId\":9002}"
        );
        assertThat(payloadJson)
                .doesNotContain("ticket-ai-reply-draft-req-1")
                .doesNotContain("gpt-4.1-mini")
                .doesNotContain("nextStep=");

        String approvalAuditSnapshot = jdbcTemplate.queryForObject(
                "SELECT after_value FROM audit_event WHERE entity_type = 'APPROVAL_REQUEST' AND entity_id = ? AND action_type = 'APPROVAL_REQUEST_CREATED'",
                String.class,
                approvalRequestId
        );
        assertThat(approvalAuditSnapshot)
                .contains("TICKET_COMMENT_CREATE")
                .contains("\\\"commentContent\\\":\\\"Draft reply for the store. Cable swap verified.\\\"")
                .contains("\\\"sourceInteractionId\\\":9002")
                .doesNotContain("ticket-ai-reply-draft-req-1")
                .doesNotContain("gpt-4.1-mini")
                .doesNotContain("nextStep=");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticket_comment WHERE ticket_id = ?",
                Integer.class,
                301L
        )).isZero();
    }

    @Test
    void createCommentProposalShouldRejectDuplicatePendingProposalForSameTrimmedComment() throws Exception {
        String opsToken = loginAndGetToken("demo-shop", "ops", "123456");

        Long approvalRequestId = createCommentProposalAndGetId(
                opsToken,
                "ticket-comment-proposal-duplicate-first",
                "  Draft reply for the store. Cable swap verified.  ",
                9002L
        );

        mockMvc.perform(post("/api/v1/tickets/301/comments/proposals/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(opsToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-duplicate-second")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentProposalRequest("Draft reply for the store. Cable swap verified.", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("pending ticket comment proposal already exists for ticket and comment content"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT payload_json FROM approval_request WHERE id = ?",
                String.class,
                approvalRequestId
        )).isEqualTo("{\"commentContent\":\"Draft reply for the store. Cable swap verified.\",\"sourceInteractionId\":9002}");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM approval_request WHERE action_type = 'TICKET_COMMENT_CREATE'",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM approval_request WHERE action_type = 'TICKET_COMMENT_CREATE' AND status = 'PENDING'",
                Integer.class
        )).isEqualTo(1);
    }

    @Test
    void createCommentProposalShouldRequireTicketWriteAndHideMissingOrCrossTenantTickets() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");
        String opsToken = loginAndGetToken("demo-shop", "ops", "123456");
        String outsiderToken = loginAndGetToken("other-shop", "outsider", "123456");

        mockMvc.perform(post("/api/v1/tickets/301/comments/proposals/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-read-denied")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentProposalRequest("Reply draft from viewer", 9002L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("permission denied"));

        mockMvc.perform(post("/api/v1/tickets/999/comments/proposals/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(opsToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-missing-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentProposalRequest("Reply draft for missing ticket", 9002L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("ticket not found"));

        mockMvc.perform(post("/api/v1/tickets/301/comments/proposals/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(outsiderToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-cross-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentProposalRequest("Cross-tenant reply draft", null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("ticket not found"));
    }

    @Test
    void createCommentProposalShouldReturnServiceUnavailableWhenBridgeFlagDisabled() throws Exception {
        setFeatureFlag("workflow.ticket.comment-proposal.enabled", false);
        String opsToken = loginAndGetToken("demo-shop", "ops", "123456");

        mockMvc.perform(post("/api/v1/tickets/301/comments/proposals/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(opsToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-flag-disabled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentProposalRequest("Reply draft content", 9002L)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("ticket comment proposal is disabled"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM approval_request WHERE action_type = 'TICKET_COMMENT_CREATE'",
                Integer.class
        )).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_event", Integer.class)).isZero();
    }

    @Test
    void createCommentProposalShouldRejectBlankOverlongAndInvalidSourceInteraction() throws Exception {
        String opsToken = loginAndGetToken("demo-shop", "ops", "123456");

        mockMvc.perform(post("/api/v1/tickets/301/comments/proposals/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(opsToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-blank")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentProposalRequest("   ", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("commentContent: commentContent must not be blank"));

        mockMvc.perform(post("/api/v1/tickets/301/comments/proposals/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(opsToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-too-long")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentProposalRequest("a".repeat(2001), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("commentContent: commentContent length must be less than or equal to 2000"));

        mockMvc.perform(post("/api/v1/tickets/301/comments/proposals/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(opsToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-invalid-source-type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentProposalRequest("Reply draft content", 9003L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("sourceInteractionId must reference a succeeded REPLY_DRAFT interaction for the source ticket"));

        mockMvc.perform(post("/api/v1/tickets/301/comments/proposals/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(opsToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-invalid-source-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentProposalRequest("Reply draft content", 9004L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("sourceInteractionId must reference a succeeded REPLY_DRAFT interaction for the source ticket"));
    }

    @Test
    void createCommentProposalShouldAllowSamePayloadAgainAfterRejectingResolvedProposal() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");
        String opsToken = loginAndGetToken("demo-shop", "ops", "123456");

        Long firstApprovalRequestId = createCommentProposalAndGetId(
                opsToken,
                "ticket-comment-proposal-retry-after-reject-first",
                "Rejectable repeat draft",
                9002L
        );

        mockMvc.perform(post("/api/v1/approval-requests/{id}/reject", firstApprovalRequestId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-retry-after-reject-review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        Long secondApprovalRequestId = createCommentProposalAndGetId(
                opsToken,
                "ticket-comment-proposal-retry-after-reject-second",
                "  Rejectable repeat draft  ",
                null
        );

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
                "SELECT COUNT(*) FROM approval_request WHERE action_type = 'TICKET_COMMENT_CREATE'",
                Integer.class
        )).isEqualTo(2);
    }

    @Test
    void createCommentProposalShouldAllowSamePayloadAgainAfterApprovingResolvedProposal() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");
        String opsToken = loginAndGetToken("demo-shop", "ops", "123456");

        Long firstApprovalRequestId = createCommentProposalAndGetId(
                opsToken,
                "ticket-comment-proposal-retry-after-approve-first",
                "Approved repeat draft",
                null
        );

        mockMvc.perform(post("/api/v1/approval-requests/{id}/approve", firstApprovalRequestId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-retry-after-approve-review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        Long secondApprovalRequestId = createCommentProposalAndGetId(
                opsToken,
                "ticket-comment-proposal-retry-after-approve-second",
                "  Approved repeat draft  ",
                9002L
        );

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
                "SELECT COUNT(*) FROM ticket_comment WHERE request_id = ?",
                Integer.class,
                "ticket-comment-proposal-retry-after-approve-review"
        )).isEqualTo(1);
    }

    @Test
    void approveCommentProposalShouldKeepSelfApprovalGuardAndCreateExactlyOneComment() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");
        String opsToken = loginAndGetToken("demo-shop", "ops", "123456");

        Long approvalRequestId = createCommentProposalAndGetId(
                opsToken,
                "ticket-comment-proposal-approve-create",
                "Reply drafted from the AI suggestion",
                9002L
        );

        mockMvc.perform(post("/api/v1/approval-requests/{id}/approve", approvalRequestId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(opsToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-self-approve"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("requester cannot approve or reject own request"));

        mockMvc.perform(post("/api/v1/approval-requests/{id}/approve", approvalRequestId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-approve-final"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM approval_request WHERE id = ?",
                String.class,
                approvalRequestId
        )).isEqualTo("APPROVED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT reviewed_by FROM approval_request WHERE id = ?",
                Long.class,
                approvalRequestId
        )).isEqualTo(101L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticket_comment WHERE ticket_id = ?",
                Integer.class,
                301L
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticket_comment WHERE ticket_id = ? AND request_id = ?",
                Integer.class,
                301L,
                "ticket-comment-proposal-approve-final"
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT content FROM ticket_comment WHERE ticket_id = ? AND request_id = ?",
                String.class,
                301L,
                "ticket-comment-proposal-approve-final"
        )).isEqualTo("Reply drafted from the AI suggestion");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT created_by FROM ticket_comment WHERE ticket_id = ? AND request_id = ?",
                Long.class,
                301L,
                "ticket-comment-proposal-approve-final"
        )).isEqualTo(101L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticket_operation_log WHERE ticket_id = ? AND request_id = ? AND operation_type = ? AND detail = ?",
                Integer.class,
                301L,
                "ticket-comment-proposal-approve-final",
                "COMMENTED",
                "comment added"
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE tenant_id = 1 AND entity_type = 'APPROVAL_REQUEST' AND entity_id = ? AND action_type = 'APPROVAL_ACTION_EXECUTED'",
                Integer.class,
                approvalRequestId
        )).isEqualTo(1);
    }

    @Test
    void approveCommentProposalShouldRecheckBridgeFlagBeforeWritingComment() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");
        String opsToken = loginAndGetToken("demo-shop", "ops", "123456");

        Long approvalRequestId = createCommentProposalAndGetId(
                opsToken,
                "ticket-comment-proposal-flag-recheck-create",
                "Reply drafted before flag shutdown",
                9002L
        );
        setFeatureFlag("workflow.ticket.comment-proposal.enabled", false);

        mockMvc.perform(post("/api/v1/approval-requests/{id}/approve", approvalRequestId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-flag-recheck-approve"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("ticket comment proposal is disabled"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM approval_request WHERE id = ?",
                String.class,
                approvalRequestId
        )).isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT reviewed_by FROM approval_request WHERE id = ?",
                Long.class,
                approvalRequestId
        )).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticket_comment WHERE request_id = ?",
                Integer.class,
                "ticket-comment-proposal-flag-recheck-approve"
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE tenant_id = 1 AND entity_type = 'APPROVAL_REQUEST' AND entity_id = ? AND action_type = 'APPROVAL_ACTION_EXECUTED'",
                Integer.class,
                approvalRequestId
        )).isZero();
    }

    @Test
    void reviewEndpointsShouldRejectAlreadyResolvedTicketCommentProposal() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");
        String opsToken = loginAndGetToken("demo-shop", "ops", "123456");

        Long approvedRequestId = createCommentProposalAndGetId(
                opsToken,
                "ticket-comment-proposal-repeat-review-approved-create",
                "Already approved draft",
                null
        );
        mockMvc.perform(post("/api/v1/approval-requests/{id}/approve", approvedRequestId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-repeat-review-approved-final"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
        mockMvc.perform(post("/api/v1/approval-requests/{id}/approve", approvedRequestId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-repeat-review-approved-repeat"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("approval request is not pending"));

        Long rejectedRequestId = createCommentProposalAndGetId(
                opsToken,
                "ticket-comment-proposal-repeat-review-rejected-create",
                "Already rejected draft",
                9002L
        );
        mockMvc.perform(post("/api/v1/approval-requests/{id}/reject", rejectedRequestId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-repeat-review-rejected-final"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
        mockMvc.perform(post("/api/v1/approval-requests/{id}/reject", rejectedRequestId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-repeat-review-rejected-repeat"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("approval request is not pending"));
    }

    @Test
    void rejectCommentProposalShouldNotCreateComment() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");
        String opsToken = loginAndGetToken("demo-shop", "ops", "123456");

        Long approvalRequestId = createCommentProposalAndGetId(
                opsToken,
                "ticket-comment-proposal-reject-create",
                "Rejectable reply draft content",
                null
        );

        mockMvc.perform(post("/api/v1/approval-requests/{id}/reject", approvalRequestId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-comment-proposal-reject-final"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM approval_request WHERE id = ?",
                String.class,
                approvalRequestId
        )).isEqualTo("REJECTED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticket_comment WHERE request_id = ?",
                Integer.class,
                "ticket-comment-proposal-reject-final"
        )).isZero();
    }

    @Test
    void createCommentProposalShouldKeepOnlyOnePendingRowUnderConcurrentDuplicateRequests() throws Exception {
        String opsToken = loginAndGetToken("demo-shop", "ops", "123456");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<MvcResult>> futures = List.of(
                    executor.submit(() -> submitConcurrentCommentProposal(
                            opsToken,
                            "ticket-comment-proposal-concurrent-1",
                            "Concurrent ticket reply draft",
                            9002L,
                            ready,
                            start
                    )),
                    executor.submit(() -> submitConcurrentCommentProposal(
                            opsToken,
                            "ticket-comment-proposal-concurrent-2",
                            "  Concurrent ticket reply draft  ",
                            null,
                            ready,
                            start
                    ))
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
                    .toList()).containsExactly("pending ticket comment proposal already exists for ticket and comment content");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM approval_request WHERE action_type = 'TICKET_COMMENT_CREATE'",
                    Integer.class
            )).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM approval_request WHERE action_type = 'TICKET_COMMENT_CREATE' AND status = 'PENDING'",
                    Integer.class
            )).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void approvalQueueShouldFilterTicketCommentActionsByPermissionSet() throws Exception {
        insertApprovalRequest(
                9101L,
                1L,
                "TICKET_COMMENT_CREATE",
                "TICKET",
                301L,
                101L,
                "PENDING",
                "{\"commentContent\":\"Seeded queue reply\",\"sourceInteractionId\":9002}",
                "seed-ticket-comment-approval-9101",
                "2026-03-30 10:00:00"
        );
        insertApprovalRequest(
                9102L,
                1L,
                "USER_STATUS_DISABLE",
                "USER",
                103L,
                101L,
                "PENDING",
                "{\"status\":\"DISABLED\"}",
                "seed-user-approval-9102",
                "2026-03-30 09:00:00"
        );
        insertApprovalRequest(
                9103L,
                1L,
                "IMPORT_JOB_SELECTIVE_REPLAY",
                "IMPORT_JOB",
                7001L,
                101L,
                "PENDING",
                "{\"sourceJobId\":7001,\"errorCodes\":[\"UNKNOWN_ROLE\"]}",
                "seed-import-approval-9103",
                "2026-03-30 08:00:00"
        );
        insertApprovalRequest(
                9201L,
                2L,
                "TICKET_COMMENT_CREATE",
                "TICKET",
                401L,
                201L,
                "PENDING",
                "{\"commentContent\":\"Other tenant reply\"}",
                "seed-other-tenant-ticket-approval-9201",
                "2026-03-30 11:00:00"
        );

        jdbcTemplate.update("DELETE FROM role_permission WHERE role_id = ? AND permission_id = ?", 13L, 1L);
        insertRole(14L, 1L, "TICKET_REVIEWER", "Ticket Reviewer");
        insertUser(104L, 1L, "ticketreviewer", passwordEncoder.encode("123456"), "Ticket Reviewer", "ticketreviewer@demo-shop.local", "ACTIVE");
        insertUserRole(1005L, 104L, 14L);
        insertRolePermission(2018L, 14L, 6L);
        insertRolePermission(2019L, 14L, 7L);

        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");
        String ticketReviewerToken = loginAndGetToken("demo-shop", "ticketreviewer", "123456");
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");

        mockMvc.perform(get("/api/v1/approval-requests")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("page", "0")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[*].id", contains(9101)))
                .andExpect(jsonPath("$.data.items[*].id", not(hasItem(9201))));

        mockMvc.perform(get("/api/v1/approval-requests/9101")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actionType").value("TICKET_COMMENT_CREATE"));

        mockMvc.perform(post("/api/v1/approval-requests/9101/approve")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "viewer-approve-ticket-comment"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("permission denied"));

        mockMvc.perform(get("/api/v1/approval-requests")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(ticketReviewerToken))
                        .queryParam("page", "0")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[*].id", contains(9101)));

        mockMvc.perform(get("/api/v1/approval-requests")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(ticketReviewerToken))
                        .queryParam("actionType", "USER_STATUS_DISABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/v1/approval-requests/9102")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(ticketReviewerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("approval request not found"));

        mockMvc.perform(post("/api/v1/approval-requests/9102/approve")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(ticketReviewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-only-approve-user-request"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("approval request not found"));

        mockMvc.perform(post("/api/v1/approval-requests/9101/approve")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(ticketReviewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "ticket-only-approve-ticket-request"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticket_comment WHERE ticket_id = ? AND request_id = ?",
                Integer.class,
                301L,
                "ticket-only-approve-ticket-request"
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT created_by FROM ticket_comment WHERE ticket_id = ? AND request_id = ?",
                Long.class,
                301L,
                "ticket-only-approve-ticket-request"
        )).isEqualTo(104L);

        mockMvc.perform(get("/api/v1/approval-requests")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .queryParam("page", "0")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.items[*].id", contains(9101, 9102, 9103)));

        mockMvc.perform(get("/api/v1/approval-requests")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .queryParam("actionType", "TICKET_COMMENT_CREATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[*].id", contains(9101)));
    }

    @Test
    void viewerRoleUpgradeShouldChangeTicketWriteAccessAfterRelogin() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");

        mockMvc.perform(post("/api/v1/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTicketRequest("Before upgrade", "viewer cannot do this yet")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(put("/api/v1/users/103/roles")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignRolesRequest("[\"OPS_USER\"]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCodes[0]").value("OPS_USER"))
                .andExpect(jsonPath("$.data.permissionCodes", hasItem("TICKET_WRITE")));

        mockMvc.perform(get("/api/v1/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("page", "0")
                        .queryParam("size", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("token claims are stale, please login again"));

        String refreshedViewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(refreshedViewerToken))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, "viewer-upgraded-create-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTicketRequest("Viewer can now create", "role upgrade applied")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }


    @Test
    void ticketWriteShouldCreateAuditEventAndKeepWorkflowLog() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");

        Long ticketId = createTicketAndGetId(adminToken, "audit-ticket-create-req-1", "Audit Trail Ticket", "audit desc");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM audit_event WHERE tenant_id = ? AND entity_type = ? AND entity_id = ? AND action_type = ?",
                Integer.class,
                1L,
                "TICKET",
                ticketId,
                "TICKET_CREATED"
        )).isEqualTo(1);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ticket_operation_log WHERE tenant_id = ? AND ticket_id = ?",
                Integer.class,
                1L,
                ticketId
        )).isEqualTo(1);
    }

    @Test
    void auditEventQueryShouldBeTenantScoped() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");

        jdbcTemplate.update(
                "INSERT INTO audit_event (tenant_id, entity_type, entity_id, action_type, operator_id, request_id, before_value, after_value, approval_status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                1L, "TICKET", 301L, "SEEDED_LOCAL", 101L, "seed-req-1", null, "{\"status\":\"OPEN\"}", "NOT_REQUIRED"
        );
        jdbcTemplate.update(
                "INSERT INTO audit_event (tenant_id, entity_type, entity_id, action_type, operator_id, request_id, before_value, after_value, approval_status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                2L, "TICKET", 301L, "SEEDED_OTHER", 201L, "seed-req-2", null, "{\"status\":\"OPEN\"}", "NOT_REQUIRED"
        );

        mockMvc.perform(get("/api/v1/audit-events")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .queryParam("entityType", "TICKET")
                        .queryParam("entityId", "301"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].actionType").value("SEEDED_LOCAL"))
                .andExpect(jsonPath("$.data.items[0].requestId").value("seed-req-1"));
    }

    @Test
    void auditEventQueryShouldAcceptCaseInsensitiveEntityType() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");

        jdbcTemplate.update(
                "INSERT INTO audit_event (tenant_id, entity_type, entity_id, action_type, operator_id, request_id, before_value, after_value, approval_status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                1L, "TICKET", 302L, "SEEDED_LOCAL", 101L, "seed-req-3", null, "{\"status\":\"IN_PROGRESS\"}", "NOT_REQUIRED"
        );

        mockMvc.perform(get("/api/v1/audit-events")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .queryParam("entityType", "ticket")
                        .queryParam("entityId", "302"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].entityType").value("TICKET"))
                .andExpect(jsonPath("$.data.items[0].requestId").value("seed-req-3"));
    }

    @Test
    void auditEventTableShouldRejectCrossTenantOperatorLinkage() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO audit_event (tenant_id, entity_type, entity_id, action_type, operator_id, request_id, before_value, after_value, approval_status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                1L, "TICKET", 301L, "ILLEGAL_OPERATOR_LINK", 201L, "seed-bad-req-1", null, "{\"status\":\"OPEN\"}", "NOT_REQUIRED"
        )).isInstanceOf(DataIntegrityViolationException.class);
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

    private void seedPermissions() {
        insertPermission(1L, "USER_READ", "Read user");
        insertPermission(2L, "USER_WRITE", "Write user");
        insertPermission(3L, "ORDER_READ", "Read order");
        insertPermission(4L, "BILLING_READ", "Read billing");
        insertPermission(5L, "FEATURE_FLAG_MANAGE", "Manage feature flag");
        insertPermission(6L, "TICKET_READ", "Read ticket");
        insertPermission(7L, "TICKET_WRITE", "Write ticket");
    }

    private void seedRoles() {
        insertRole(11L, 1L, "TENANT_ADMIN", "Tenant Admin");
        insertRole(12L, 1L, "OPS_USER", "Operations User");
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
        insertUserRole(1002L, 102L, 12L);
        insertUserRole(1003L, 103L, 13L);
        insertUserRole(1004L, 201L, 21L);
    }

    private void seedRolePermissions() {
        insertRolePermission(2001L, 11L, 1L);
        insertRolePermission(2002L, 11L, 2L);
        insertRolePermission(2003L, 11L, 3L);
        insertRolePermission(2004L, 11L, 4L);
        insertRolePermission(2005L, 11L, 5L);
        insertRolePermission(2006L, 11L, 6L);
        insertRolePermission(2007L, 11L, 7L);
        insertRolePermission(2008L, 12L, 1L);
        insertRolePermission(2009L, 12L, 3L);
        insertRolePermission(2010L, 12L, 6L);
        insertRolePermission(2011L, 12L, 7L);
        insertRolePermission(2012L, 13L, 1L);
        insertRolePermission(2013L, 13L, 6L);
        insertRolePermission(2014L, 21L, 1L);
        insertRolePermission(2015L, 21L, 2L);
        insertRolePermission(2016L, 21L, 6L);
        insertRolePermission(2017L, 21L, 7L);
    }

    private void seedTickets() {
        insertTicket(301L, 1L, "POS printer offline", "Investigate printer outage", "OPEN", null, 101L, "seed-ticket-301");
        insertTicket(302L, 1L, "Printer cable replacement", "Replace damaged cable", "IN_PROGRESS", 102L, 101L, "seed-ticket-302");
        insertTicket(303L, 1L, "Cash drawer issue", "Drawer fix completed", "CLOSED", 102L, 101L, "seed-ticket-303");
        insertTicket(401L, 2L, "Other tenant ticket", "Other tenant issue", "OPEN", null, 201L, "seed-ticket-401");
    }

    private void seedAiInteractions() {
        insertAiInteractionRecord(
                9002L,
                1L,
                102L,
                "ticket-ai-reply-draft-req-1",
                "TICKET",
                301L,
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
                Timestamp.valueOf("2026-03-22 09:00:00")
        );
        insertAiInteractionRecord(
                9003L,
                1L,
                102L,
                "ticket-ai-triage-req-1",
                "TICKET",
                301L,
                "TRIAGE",
                "ticket-triage-v1",
                "gpt-4.1-mini",
                "SUCCEEDED",
                251L,
                "triage severity=medium",
                120,
                44,
                164,
                1500L,
                Timestamp.valueOf("2026-03-22 09:05:00")
        );
        insertAiInteractionRecord(
                9004L,
                1L,
                102L,
                "ticket-ai-reply-draft-other-ticket-1",
                "TICKET",
                302L,
                "REPLY_DRAFT",
                "ticket-reply-draft-v1",
                "gpt-4.1-mini",
                "SUCCEEDED",
                401L,
                "other ticket reply draft",
                118,
                62,
                180,
                1700L,
                Timestamp.valueOf("2026-03-22 09:10:00")
        );
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
                                           Timestamp createdAt) {
        jdbcTemplate.update("""
                INSERT INTO ai_interaction_record (
                    id, tenant_id, user_id, request_id, entity_type, entity_id, interaction_type, prompt_version,
                    model_id, status, latency_ms, output_summary, usage_prompt_tokens, usage_completion_tokens,
                    usage_total_tokens, usage_cost_micros, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id, tenantId, userId, requestId, entityType, entityId, interactionType, promptVersion,
                modelId, status, latencyMs, outputSummary, usagePromptTokens, usageCompletionTokens,
                usageTotalTokens, usageCostMicros, createdAt
        );
    }

    private void insertApprovalRequest(Long id,
                                       Long tenantId,
                                       String actionType,
                                       String entityType,
                                       Long entityId,
                                       Long requestedBy,
                                       String status,
                                       String payloadJson,
                                       String requestId,
                                       String createdAt) {
        jdbcTemplate.update("""
                INSERT INTO approval_request (
                    id, tenant_id, action_type, entity_type, entity_id, requested_by, reviewed_by, status,
                    payload_json, pending_request_key, request_id, created_at, reviewed_at, executed_at
                ) VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?, NULL, ?, PARSEDATETIME(?, 'yyyy-MM-dd HH:mm:ss'), NULL, NULL)
                """,
                id, tenantId, actionType, entityType, entityId, requestedBy, status, payloadJson, requestId, createdAt
        );
    }

    private Long createTicketAndGetId(String token, String requestId, String title, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTicketRequest(title, description)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return root.path("data").path("id").asLong();
    }

    private Long createCommentProposalAndGetId(String token, String requestId, String commentContent, Long sourceInteractionId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tickets/301/comments/proposals/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentProposalRequest(commentContent, sourceInteractionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return root.path("data").path("id").asLong();
    }

    private MvcResult submitConcurrentCommentProposal(String token,
                                                      String requestId,
                                                      String commentContent,
                                                      Long sourceInteractionId,
                                                      CountDownLatch ready,
                                                      CountDownLatch start) throws Exception {
        ready.countDown();
        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
        return mockMvc.perform(post("/api/v1/tickets/301/comments/proposals/ai-reply-draft")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .header(RequestIdFilter.REQUEST_ID_HEADER, requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentProposalRequest(commentContent, sourceInteractionId)))
                .andReturn();
    }

    private String loginAndGetToken(String tenantCode, String username, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
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

    private String createTicketRequest(String title, String description) {
        return """
                {
                  "title": "%s",
                  "description": "%s"
                }
                """.formatted(title, description);
    }

    private String assignTicketRequest(Long assigneeId) {
        return """
                {
                  "assigneeId": %d
                }
                """.formatted(assigneeId);
    }

    private String statusUpdateRequest(String status) {
        return """
                {
                  "status": "%s"
                }
                """.formatted(status);
    }

    private String commentRequest(String content) {
        return """
                {
                  "content": "%s"
                }
                """.formatted(content);
    }

    private String commentProposalRequest(String commentContent, Long sourceInteractionId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("commentContent", commentContent);
        if (sourceInteractionId != null) {
            payload.put("sourceInteractionId", sourceInteractionId);
        }
        return objectMapper.writeValueAsString(payload);
    }

    private String assignRolesRequest(String roleCodesJson) {
        return """
                {
                  "roleCodes": %s
                }
                """.formatted(roleCodesJson);
    }

    private String bearerToken(String token) {
        return "Bearer " + token;
    }

    private String responseMessage(MvcResult result) {
        try {
            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
            return root.path("message").asText();
        } catch (Exception ex) {
            throw new IllegalStateException("failed to parse response message", ex);
        }
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
