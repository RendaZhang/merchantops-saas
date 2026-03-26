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
        seedTickets();
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
