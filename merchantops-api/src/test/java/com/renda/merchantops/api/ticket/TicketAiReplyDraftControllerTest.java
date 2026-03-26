package com.renda.merchantops.api.ticket;

import com.renda.merchantops.api.context.CurrentUserContext;
import com.renda.merchantops.api.context.TenantContext;
import com.renda.merchantops.api.dto.ticket.query.TicketAiReplyDraftResponse;
import com.renda.merchantops.api.exception.GlobalExceptionHandler;
import com.renda.merchantops.api.filter.RequestIdFilter;
import com.renda.merchantops.api.security.CurrentUser;
import com.renda.merchantops.api.security.RequirePermissionInterceptor;
import com.renda.merchantops.api.ticket.ai.TicketAiReplyDraftService;
import com.renda.merchantops.api.ticket.ai.TicketAiSummaryService;
import com.renda.merchantops.api.ticket.ai.TicketAiTriageService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TicketAiReplyDraftControllerTest {

    private static final String HEADER_AUTH = "X-Test-Auth";
    private static final String HEADER_AUTHORITIES = "X-Test-Authorities";
    private static final String HEADER_TENANT_ID = "X-Test-Tenant-Id";
    private static final String HEADER_TENANT_CODE = "X-Test-Tenant-Code";
    private static final String HEADER_USER_ID = "X-Test-User-Id";
    private static final String HEADER_REQUEST_ID = "X-Test-Request-Id";

    @Mock
    private TicketQueryService ticketQueryService;

    @Mock
    private TicketAiSummaryService ticketAiSummaryService;

    @Mock
    private TicketAiTriageService ticketAiTriageService;

    @Mock
    private TicketAiReplyDraftService ticketAiReplyDraftService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new TicketAiController(ticketQueryService, ticketAiSummaryService, ticketAiTriageService, ticketAiReplyDraftService)
                )
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
    void aiReplyDraftShouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/11/ai-reply-draft"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void aiReplyDraftShouldReturnForbiddenWhenPermissionIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/11/ai-reply-draft")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_READ"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void aiReplyDraftShouldForwardTenantUserAndRequestId() throws Exception {
        TicketAiReplyDraftResponse response = new TicketAiReplyDraftResponse(
                11L,
                "Quick update from ops.\n\nWork is still in progress on the printer issue.\n\nNext step: Confirm whether the cable replacement resolved the outage.\n\nI will add another note once verification is complete.",
                "Quick update from ops.",
                "Work is still in progress on the printer issue.",
                "Confirm whether the cable replacement resolved the outage.",
                "I will add another note once verification is complete.",
                "ticket-reply-draft-v1",
                "gpt-4.1-mini",
                LocalDateTime.of(2026, 3, 21, 15, 10, 15),
                436L,
                "ticket-ai-reply-draft-req-1"
        );
        when(ticketAiReplyDraftService.generateReplyDraft(9L, 9001L, "ticket-ai-reply-draft-req-1", 11L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/tickets/11/ai-reply-draft")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_USER_ID, "9001")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "TICKET_READ")
                        .header(HEADER_REQUEST_ID, "ticket-ai-reply-draft-req-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.ticketId").value(11))
                .andExpect(jsonPath("$.data.nextStep").value("Confirm whether the cable replacement resolved the outage."))
                .andExpect(jsonPath("$.data.requestId").value("ticket-ai-reply-draft-req-1"));

        verify(ticketAiReplyDraftService).generateReplyDraft(eq(9L), eq(9001L), eq("ticket-ai-reply-draft-req-1"), eq(11L));
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
