package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.context.CurrentUserContext;
import com.renda.merchantops.api.context.TenantContext;
import com.renda.merchantops.api.dto.importjob.query.ImportJobDetailResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobPageResponse;
import com.renda.merchantops.api.exception.GlobalExceptionHandler;
import com.renda.merchantops.api.filter.RequestIdFilter;
import com.renda.merchantops.api.security.CurrentUser;
import com.renda.merchantops.api.security.RequirePermissionInterceptor;
import com.renda.merchantops.api.service.ImportJobCommandService;
import com.renda.merchantops.api.service.ImportJobQueryService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ImportJobControllerTest {

    private static final String HEADER_AUTH = "X-Test-Auth";
    private static final String HEADER_AUTHORITIES = "X-Test-Authorities";
    private static final String HEADER_TENANT_ID = "X-Test-Tenant-Id";
    private static final String HEADER_TENANT_CODE = "X-Test-Tenant-Code";
    private static final String HEADER_USER_ID = "X-Test-User-Id";
    private static final String HEADER_REQUEST_ID = "X-Test-Request-Id";

    @Mock
    private ImportJobCommandService importJobCommandService;
    @Mock
    private ImportJobQueryService importJobQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ImportJobController controller = new ImportJobController(importJobCommandService, importJobQueryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
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
    void listShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/import-jobs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createShouldBindMultipartAndForwardContext() throws Exception {
        when(importJobCommandService.createJob(eq(9L), eq(9001L), eq("req-import-1"), any(), any()))
                .thenReturn(new ImportJobDetailResponse(1L, 9L, "USER_CSV", "CSV", "users.csv", "9/key.csv", "QUEUED", 9001L,
                        "req-import-1", 0, 0, 0, null, null, null, null, List.of()));

        MockMultipartFile requestPart = new MockMultipartFile("request", "", "application/json", "{\"importType\":\"USER_CSV\"}".getBytes());
        MockMultipartFile filePart = new MockMultipartFile("file", "users.csv", "text/csv", "username,email\na,a@x.com".getBytes());

        mockMvc.perform(multipart("/api/v1/import-jobs")
                        .file(requestPart)
                        .file(filePart)
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_USER_ID, "9001")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_WRITE")
                        .header(HEADER_REQUEST_ID, "req-import-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("QUEUED"));

        verify(importJobCommandService).createJob(eq(9L), eq(9001L), eq("req-import-1"), any(), any());
    }

    @Test
    void listShouldRequirePermission() throws Exception {
        mockMvc.perform(get("/api/v1/import-jobs")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_WRITE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listShouldReturnTenantScopedPage() throws Exception {
        when(importJobQueryService.pageJobs(eq(9L), any()))
                .thenReturn(new ImportJobPageResponse(List.of(), 0, 10, 0, 0));

        mockMvc.perform(get("/api/v1/import-jobs")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_READ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    private static class TestAuthenticationFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            String requestId = request.getHeader(HEADER_REQUEST_ID);
            if (StringUtils.hasText(requestId)) {
                MDC.put(RequestIdFilter.MDC_KEY, requestId.trim());
            }

            if ("true".equalsIgnoreCase(request.getHeader(HEADER_AUTH))) {
                Long userId = parseLong(request.getHeader(HEADER_USER_ID), 9001L);
                Long tenantId = parseLong(request.getHeader(HEADER_TENANT_ID), 9L);
                String tenantCode = request.getHeader(HEADER_TENANT_CODE);
                if (!StringUtils.hasText(tenantCode)) {
                    tenantCode = "demo-shop";
                }
                String authoritiesHeader = request.getHeader(HEADER_AUTHORITIES);
                List<SimpleGrantedAuthority> authorities = Stream.of(StringUtils.hasText(authoritiesHeader) ? authoritiesHeader.split(",") : new String[0])
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken("tester", null, authorities)
                );
                CurrentUserContext.set(new CurrentUser(
                        userId,
                        tenantId,
                        tenantCode,
                        "tester",
                        List.of(),
                        List.of()
                ));
                if (StringUtils.hasText(request.getHeader(HEADER_TENANT_ID))) {
                    TenantContext.setTenant(tenantId, tenantCode);
                }
            }

            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
                CurrentUserContext.clear();
                TenantContext.clear();
                MDC.remove(RequestIdFilter.MDC_KEY);
            }
        }

        private static Long parseLong(String value, Long defaultValue) {
            if (!StringUtils.hasText(value)) {
                return defaultValue;
            }
            return Long.parseLong(value);
        }
    }
}
