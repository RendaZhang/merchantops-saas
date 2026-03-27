package com.renda.merchantops.api.importjob;

import com.renda.merchantops.api.context.CurrentUserContext;
import com.renda.merchantops.api.context.TenantContext;
import com.renda.merchantops.api.dto.importjob.query.ImportJobAiErrorSummaryResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobAiMappingSuggestionResponse;
import com.renda.merchantops.api.exception.GlobalExceptionHandler;
import com.renda.merchantops.api.filter.RequestIdFilter;
import com.renda.merchantops.api.importjob.ai.ImportJobAiErrorSummaryService;
import com.renda.merchantops.api.importjob.ai.ImportJobAiMappingSuggestionService;
import com.renda.merchantops.api.security.CurrentUser;
import com.renda.merchantops.api.security.RequirePermissionInterceptor;
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
class ImportJobAiControllerTest {

    private static final String HEADER_AUTH = "X-Test-Auth";
    private static final String HEADER_AUTHORITIES = "X-Test-Authorities";
    private static final String HEADER_TENANT_ID = "X-Test-Tenant-Id";
    private static final String HEADER_TENANT_CODE = "X-Test-Tenant-Code";
    private static final String HEADER_USER_ID = "X-Test-User-Id";
    private static final String HEADER_REQUEST_ID = "X-Test-Request-Id";

    @Mock
    private ImportJobAiErrorSummaryService importJobAiErrorSummaryService;

    @Mock
    private ImportJobAiMappingSuggestionService importJobAiMappingSuggestionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ImportJobAiController(
                        importJobAiErrorSummaryService,
                        importJobAiMappingSuggestionService
                ))
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
    void aiErrorSummaryShouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/import-jobs/11/ai-error-summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void aiErrorSummaryShouldReturnForbiddenWhenPermissionIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/import-jobs/11/ai-error-summary")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_WRITE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void aiErrorSummaryShouldForwardTenantUserAndRequestId() throws Exception {
        ImportJobAiErrorSummaryResponse response = new ImportJobAiErrorSummaryResponse(
                11L,
                "The job is dominated by role validation failures.",
                List.of("UNKNOWN_ROLE is the dominant failure family."),
                List.of("Correct tenant role mappings before replay."),
                "import-error-summary-v1",
                "gpt-4.1-mini",
                LocalDateTime.of(2026, 3, 27, 10, 20, 15),
                512L,
                "import-ai-error-summary-req-1"
        );
        when(importJobAiErrorSummaryService.generateErrorSummary(9L, 9001L, "import-ai-error-summary-req-1", 11L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/import-jobs/11/ai-error-summary")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_USER_ID, "9001")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_READ")
                        .header(HEADER_REQUEST_ID, "import-ai-error-summary-req-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.importJobId").value(11))
                .andExpect(jsonPath("$.data.requestId").value("import-ai-error-summary-req-1"));

        verify(importJobAiErrorSummaryService).generateErrorSummary(eq(9L), eq(9001L), eq("import-ai-error-summary-req-1"), eq(11L));
    }

    @Test
    void aiMappingSuggestionShouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/import-jobs/11/ai-mapping-suggestion"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void aiMappingSuggestionShouldReturnForbiddenWhenPermissionIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/import-jobs/11/ai-mapping-suggestion")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_WRITE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void aiMappingSuggestionShouldForwardTenantUserAndRequestId() throws Exception {
        ImportJobAiMappingSuggestionResponse response = new ImportJobAiMappingSuggestionResponse(
                11L,
                "The failed header still looks close to USER_CSV, so the operator should confirm the proposed mappings before any replay preparation.",
                List.of(
                        new ImportJobAiMappingSuggestionResponse.SuggestedFieldMapping(
                                "username",
                                new ImportJobAiMappingSuggestionResponse.ObservedColumnSignal("login", 1),
                                "`login` is the closest username signal.",
                                false
                        ),
                        new ImportJobAiMappingSuggestionResponse.SuggestedFieldMapping(
                                "displayName",
                                new ImportJobAiMappingSuggestionResponse.ObservedColumnSignal("display_name", 2),
                                "`display_name` is the closest displayName signal.",
                                false
                        ),
                        new ImportJobAiMappingSuggestionResponse.SuggestedFieldMapping(
                                "email",
                                new ImportJobAiMappingSuggestionResponse.ObservedColumnSignal("email_address", 3),
                                "`email_address` is the closest email signal.",
                                false
                        ),
                        new ImportJobAiMappingSuggestionResponse.SuggestedFieldMapping(
                                "password",
                                new ImportJobAiMappingSuggestionResponse.ObservedColumnSignal("passwd", 4),
                                "`passwd` should be manually confirmed.",
                                true
                        ),
                        new ImportJobAiMappingSuggestionResponse.SuggestedFieldMapping(
                                "roleCodes",
                                new ImportJobAiMappingSuggestionResponse.ObservedColumnSignal("roles", 5),
                                "`roles` likely maps to roleCodes.",
                                true
                        )
                ),
                List.of("The source file failed header validation, so each mapping should be reviewed before reuse."),
                List.of("Confirm the observed header order before editing any replay input."),
                "import-mapping-suggestion-v1",
                "gpt-4.1-mini",
                LocalDateTime.of(2026, 3, 27, 10, 30, 15),
                544L,
                "import-ai-mapping-suggestion-req-1"
        );
        when(importJobAiMappingSuggestionService.generateMappingSuggestion(9L, 9001L, "import-ai-mapping-suggestion-req-1", 11L))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/import-jobs/11/ai-mapping-suggestion")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_USER_ID, "9001")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_READ")
                        .header(HEADER_REQUEST_ID, "import-ai-mapping-suggestion-req-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.importJobId").value(11))
                .andExpect(jsonPath("$.data.suggestedFieldMappings[0].canonicalField").value("username"))
                .andExpect(jsonPath("$.data.requestId").value("import-ai-mapping-suggestion-req-1"));

        verify(importJobAiMappingSuggestionService)
                .generateMappingSuggestion(eq(9L), eq(9001L), eq("import-ai-mapping-suggestion-req-1"), eq(11L));
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
