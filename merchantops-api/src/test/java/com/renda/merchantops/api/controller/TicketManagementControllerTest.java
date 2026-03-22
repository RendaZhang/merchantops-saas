package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.context.CurrentUserContext;
import com.renda.merchantops.api.context.TenantContext;
import com.renda.merchantops.api.dto.ticket.command.TicketAssigneeUpdateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketCommentCreateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketCreateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketStatusUpdateCommand;
import com.renda.merchantops.api.dto.ticket.command.TicketWriteResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketAiInteractionListItemResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketAiInteractionPageQuery;
import com.renda.merchantops.api.dto.ticket.query.TicketAiInteractionPageResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketCommentResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketDetailResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketListItemResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketOperationLogResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketPageQuery;
import com.renda.merchantops.api.dto.ticket.query.TicketPageResponse;
import com.renda.merchantops.api.exception.GlobalExceptionHandler;
import com.renda.merchantops.api.filter.RequestIdFilter;
import com.renda.merchantops.api.security.CurrentUser;
import com.renda.merchantops.api.security.RequirePermissionInterceptor;
import com.renda.merchantops.api.service.TicketAiReplyDraftService;
import com.renda.merchantops.api.service.TicketAiTriageService;
import com.renda.merchantops.api.service.TicketAiSummaryService;
import com.renda.merchantops.api.service.TicketCommandService;
import com.renda.merchantops.api.service.TicketQueryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TicketManagementControllerTest {

    private static final String HEADER_AUTH = "X-Test-Auth";
    private static final String HEADER_AUTHORITIES = "X-Test-Authorities";
    private static final String HEADER_TENANT_ID = "X-Test-Tenant-Id";
    private static final String HEADER_TENANT_CODE = "X-Test-Tenant-Code";
    private static final String HEADER_USER_ID = "X-Test-User-Id";
    private static final String HEADER_REQUEST_ID = "X-Test-Request-Id";

    @Mock
    private TicketQueryService ticketQueryService;

    @Mock
    private TicketCommandService ticketCommandService;

    @Mock
    private TicketAiSummaryService ticketAiSummaryService;

    @Mock
    private TicketAiTriageService ticketAiTriageService;

    @Mock
    private TicketAiReplyDraftService ticketAiReplyDraftService;

    @InjectMocks
    private TicketController ticketController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ticketController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(new RequirePermissionInterceptor())
                .addFilters(new TestAuthenticationFilter())
                .build();
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        MDC.remove(RequestIdFilter.MDC_KEY);
    }

    @Test
    void listTicketsShouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void listTicketsShouldReturnForbiddenWhenPermissionIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/tickets")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_READ"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("permission denied"));
    }

    @Test
    void listTicketsShouldBindQueryAndReturnPageResponse() throws Exception {
        TicketPageResponse pageResponse = new TicketPageResponse(
                List.of(new TicketListItemResponse(
                        11L,
                        "POS printer offline",
                        "OPEN",
                        102L,
                        "ops",
                        LocalDateTime.of(2026, 3, 11, 10, 0),
                        LocalDateTime.of(2026, 3, 11, 10, 5)
                )),
                1,
                5,
                6,
                2
        );

        when(ticketQueryService.pageTickets(eq(9L), any(TicketPageQuery.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/tickets")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "TICKET_READ")
                        .queryParam("page", "1")
                        .queryParam("size", "5")
                        .queryParam("status", "OPEN")
                        .queryParam("assigneeId", "102")
                        .queryParam("keyword", "printer")
                        .queryParam("unassignedOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.items[0].assigneeUsername").value("ops"));

        ArgumentCaptor<TicketPageQuery> queryCaptor = ArgumentCaptor.forClass(TicketPageQuery.class);
        verify(ticketQueryService).pageTickets(eq(9L), queryCaptor.capture());
        assertThat(queryCaptor.getValue().getPage()).isEqualTo(1);
        assertThat(queryCaptor.getValue().getSize()).isEqualTo(5);
        assertThat(queryCaptor.getValue().getStatus()).isEqualTo("OPEN");
        assertThat(queryCaptor.getValue().getAssigneeId()).isEqualTo(102L);
        assertThat(queryCaptor.getValue().getKeyword()).isEqualTo("printer");
        assertThat(queryCaptor.getValue().getUnassignedOnly()).isFalse();
    }

    @Test
    void getTicketDetailShouldReturnTicketDetail() throws Exception {
        TicketDetailResponse response = new TicketDetailResponse(
                11L,
                9L,
                "POS printer offline",
                "desc",
                "OPEN",
                102L,
                "ops",
                101L,
                "admin",
                LocalDateTime.of(2026, 3, 11, 10, 0),
                LocalDateTime.of(2026, 3, 11, 10, 5),
                List.of(new TicketCommentResponse(31L, 11L, "Printer restarted", 102L, "ops", LocalDateTime.of(2026, 3, 11, 10, 10))),
                List.of(new TicketOperationLogResponse(41L, "CREATED", "ticket created", 101L, "admin", LocalDateTime.of(2026, 3, 11, 10, 0)))
        );

        when(ticketQueryService.getTicketDetail(9L, 11L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/tickets/11")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "TICKET_READ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.comments[0].createdByUsername").value("ops"))
                .andExpect(jsonPath("$.data.operationLogs[0].operationType").value("CREATED"));

        verify(ticketQueryService).getTicketDetail(9L, 11L);
    }

    @Test
    void listAiInteractionsShouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/11/ai-interactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void listAiInteractionsShouldReturnForbiddenWhenPermissionIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/11/ai-interactions")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_READ"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("permission denied"));
    }

    @Test
    void listAiInteractionsShouldBindQueryAndReturnPageResponse() throws Exception {
        TicketAiInteractionPageResponse response = new TicketAiInteractionPageResponse(
                List.of(new TicketAiInteractionListItemResponse(
                        9003L,
                        "TRIAGE",
                        "INVALID_RESPONSE",
                        null,
                        "ticket-triage-v1",
                        "gpt-4.1-mini",
                        251L,
                        "ticket-ai-triage-invalid-response-1",
                        LocalDateTime.of(2026, 3, 22, 9, 0)
                )),
                1,
                5,
                6,
                2
        );
        when(ticketQueryService.pageTicketAiInteractions(eq(9L), eq(11L), any(TicketAiInteractionPageQuery.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/tickets/11/ai-interactions")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "TICKET_READ")
                        .queryParam("page", "1")
                        .queryParam("size", "5")
                        .queryParam("interactionType", "SUMMARY")
                        .queryParam("status", "SUCCEEDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.items[0].interactionType").value("TRIAGE"))
                .andExpect(jsonPath("$.data.items[0].status").value("INVALID_RESPONSE"));

        ArgumentCaptor<TicketAiInteractionPageQuery> queryCaptor = ArgumentCaptor.forClass(TicketAiInteractionPageQuery.class);
        verify(ticketQueryService).pageTicketAiInteractions(eq(9L), eq(11L), queryCaptor.capture());
        assertThat(queryCaptor.getValue().getPage()).isEqualTo(1);
        assertThat(queryCaptor.getValue().getSize()).isEqualTo(5);
        assertThat(queryCaptor.getValue().getInteractionType()).isEqualTo("SUMMARY");
        assertThat(queryCaptor.getValue().getStatus()).isEqualTo("SUCCEEDED");
    }

    @Test
    void createTicketShouldValidateRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "TICKET_WRITE")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "",
                                  "description": "desc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("title: title must not be blank"));
    }

    @Test
    void createTicketShouldBindRequestAndForwardRequestId() throws Exception {
        TicketWriteResponse response = new TicketWriteResponse(
                11L,
                9L,
                "POS printer offline",
                "desc",
                "OPEN",
                null,
                null,
                LocalDateTime.of(2026, 3, 11, 10, 0),
                LocalDateTime.of(2026, 3, 11, 10, 0)
        );
        when(ticketCommandService.createTicket(eq(9L), eq(9001L), eq("ticket-create-request-1"), any(TicketCreateCommand.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/tickets")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_USER_ID, "9001")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "TICKET_WRITE")
                        .header(HEADER_REQUEST_ID, "ticket-create-request-1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "POS printer offline",
                                  "description": "desc"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        ArgumentCaptor<TicketCreateCommand> commandCaptor = ArgumentCaptor.forClass(TicketCreateCommand.class);
        verify(ticketCommandService).createTicket(eq(9L), eq(9001L), eq("ticket-create-request-1"), commandCaptor.capture());
        assertThat(commandCaptor.getValue().getTitle()).isEqualTo("POS printer offline");
    }

    @Test
    void assignTicketShouldBindRequestAndForwardRequestId() throws Exception {
        TicketWriteResponse response = new TicketWriteResponse(
                11L,
                9L,
                "POS printer offline",
                "desc",
                "OPEN",
                102L,
                "ops",
                LocalDateTime.of(2026, 3, 11, 10, 0),
                LocalDateTime.of(2026, 3, 11, 10, 10)
        );
        when(ticketCommandService.assignTicket(eq(9L), eq(9001L), eq("ticket-assign-request-1"), eq(11L), any(TicketAssigneeUpdateCommand.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/tickets/11/assignee")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_USER_ID, "9001")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "TICKET_WRITE")
                        .header(HEADER_REQUEST_ID, "ticket-assign-request-1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "assigneeId": 102
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assigneeUsername").value("ops"));

        ArgumentCaptor<TicketAssigneeUpdateCommand> commandCaptor = ArgumentCaptor.forClass(TicketAssigneeUpdateCommand.class);
        verify(ticketCommandService).assignTicket(eq(9L), eq(9001L), eq("ticket-assign-request-1"), eq(11L), commandCaptor.capture());
        assertThat(commandCaptor.getValue().getAssigneeId()).isEqualTo(102L);
    }

    @Test
    void updateTicketStatusShouldRejectInvalidStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/tickets/11/status")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "TICKET_WRITE")
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "ARCHIVED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("status: status must be one of OPEN, IN_PROGRESS, CLOSED"));
    }

    @Test
    void addCommentShouldBindRequestAndForwardRequestId() throws Exception {
        TicketCommentResponse response = new TicketCommentResponse(
                31L,
                11L,
                "Printer restarted",
                102L,
                "ops",
                LocalDateTime.of(2026, 3, 11, 10, 15)
        );
        when(ticketCommandService.addComment(eq(9L), eq(9001L), eq("ticket-comment-request-1"), eq(11L), any(TicketCommentCreateCommand.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/tickets/11/comments")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_USER_ID, "9001")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "TICKET_WRITE")
                        .header(HEADER_REQUEST_ID, "ticket-comment-request-1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "content": "Printer restarted"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdByUsername").value("ops"));

        ArgumentCaptor<TicketCommentCreateCommand> commandCaptor = ArgumentCaptor.forClass(TicketCommentCreateCommand.class);
        verify(ticketCommandService).addComment(eq(9L), eq(9001L), eq("ticket-comment-request-1"), eq(11L), commandCaptor.capture());
        assertThat(commandCaptor.getValue().getContent()).isEqualTo("Printer restarted");
    }

    private static final class TestAuthenticationFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            try {
                String requestId = StringUtils.hasText(request.getHeader(HEADER_REQUEST_ID))
                        ? request.getHeader(HEADER_REQUEST_ID)
                        : "test-request-id";
                MDC.put(RequestIdFilter.MDC_KEY, requestId);

                if (StringUtils.hasText(request.getHeader(HEADER_AUTH))) {
                    Long userId = StringUtils.hasText(request.getHeader(HEADER_USER_ID))
                            ? Long.valueOf(request.getHeader(HEADER_USER_ID))
                            : 9001L;
                    List<SimpleGrantedAuthority> authorities = Stream.of(
                                    request.getHeader(HEADER_AUTHORITIES) == null
                                            ? new String[0]
                                            : request.getHeader(HEADER_AUTHORITIES).split(",")
                            )
                            .map(String::trim)
                            .filter(StringUtils::hasText)
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken("tester", null, authorities)
                    );
                    CurrentUserContext.set(new CurrentUser(
                            userId,
                            StringUtils.hasText(request.getHeader(HEADER_TENANT_ID))
                                    ? Long.valueOf(request.getHeader(HEADER_TENANT_ID))
                                    : null,
                            request.getHeader(HEADER_TENANT_CODE),
                            "tester",
                            List.of(),
                            List.of()
                    ));
                }

                if (StringUtils.hasText(request.getHeader(HEADER_TENANT_ID))) {
                    TenantContext.setTenant(
                            Long.valueOf(request.getHeader(HEADER_TENANT_ID)),
                            request.getHeader(HEADER_TENANT_CODE)
                    );
                }

                filterChain.doFilter(request, response);
            } finally {
                MDC.remove(RequestIdFilter.MDC_KEY);
                CurrentUserContext.clear();
                SecurityContextHolder.clearContext();
                TenantContext.clear();
            }
        }
    }
}
