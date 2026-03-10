package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.context.TenantContext;
import com.renda.merchantops.api.dto.role.query.RoleListItemResponse;
import com.renda.merchantops.api.dto.role.query.RoleListResponse;
import com.renda.merchantops.api.exception.GlobalExceptionHandler;
import com.renda.merchantops.api.security.RequirePermissionInterceptor;
import com.renda.merchantops.api.service.RoleQueryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    private static final String HEADER_AUTH = "X-Test-Auth";
    private static final String HEADER_AUTHORITIES = "X-Test-Authorities";
    private static final String HEADER_TENANT_ID = "X-Test-Tenant-Id";
    private static final String HEADER_TENANT_CODE = "X-Test-Tenant-Code";

    @Mock
    private RoleQueryService roleQueryService;

    @InjectMocks
    private RoleController roleController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(new RequirePermissionInterceptor())
                .addFilters(new TestAuthenticationFilter())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void listRolesShouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void listRolesShouldReturnForbiddenWhenPermissionIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/roles")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_READ"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("permission denied"));
    }

    @Test
    void listRolesShouldReturnTenantRoles() throws Exception {
        when(roleQueryService.listRoles(9L)).thenReturn(new RoleListResponse(List.of(
                new RoleListItemResponse(11L, "TENANT_ADMIN", "Tenant Admin"),
                new RoleListItemResponse(13L, "READ_ONLY", "Read Only User")
        )));

        mockMvc.perform(get("/api/v1/roles")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_WRITE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].roleCode").value("TENANT_ADMIN"))
                .andExpect(jsonPath("$.data.items[1].roleCode").value("READ_ONLY"));

        verify(roleQueryService).listRoles(9L);
    }

    private static final class TestAuthenticationFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            try {
                if (StringUtils.hasText(request.getHeader(HEADER_AUTH))) {
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
                }

                if (StringUtils.hasText(request.getHeader(HEADER_TENANT_ID))) {
                    TenantContext.setTenant(
                            Long.valueOf(request.getHeader(HEADER_TENANT_ID)),
                            request.getHeader(HEADER_TENANT_CODE)
                    );
                }

                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
                TenantContext.clear();
            }
        }
    }
}
