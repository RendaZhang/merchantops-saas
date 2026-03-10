package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.context.TenantContext;
import com.renda.merchantops.api.dto.user.command.UserCreateCommand;
import com.renda.merchantops.api.dto.user.command.UserCreateResponse;
import com.renda.merchantops.api.dto.user.command.UserRoleAssignmentCommand;
import com.renda.merchantops.api.dto.user.command.UserRoleAssignmentResponse;
import com.renda.merchantops.api.dto.user.command.UserStatusUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserWriteResponse;
import com.renda.merchantops.api.dto.user.query.UserDetailResponse;
import com.renda.merchantops.api.dto.user.query.UserListItemResponse;
import com.renda.merchantops.api.dto.user.query.UserPageQuery;
import com.renda.merchantops.api.dto.user.query.UserPageResponse;
import com.renda.merchantops.api.exception.GlobalExceptionHandler;
import com.renda.merchantops.api.security.RequirePermissionInterceptor;
import com.renda.merchantops.api.service.UserCommandService;
import com.renda.merchantops.api.service.UserQueryService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserManagementControllerTest {

    private static final String HEADER_AUTH = "X-Test-Auth";
    private static final String HEADER_AUTHORITIES = "X-Test-Authorities";
    private static final String HEADER_TENANT_ID = "X-Test-Tenant-Id";
    private static final String HEADER_TENANT_CODE = "X-Test-Tenant-Code";

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private UserCommandService userCommandService;

    @InjectMocks
    private UserManagementController userManagementController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userManagementController)
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
    void listUsersShouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("authentication required"));
    }

    @Test
    void listUsersShouldReturnForbiddenWhenUserLacksPermission() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "ORDER_READ"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("permission denied"));
    }

    @Test
    void listUsersShouldReturnUnauthorizedWhenTenantContextIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_AUTHORITIES, "USER_READ"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("tenant context missing"));
    }

    @Test
    void listUsersShouldBindQueryAndReturnPageResponse() throws Exception {
        UserPageResponse pageResponse = new UserPageResponse(
                List.of(new UserListItemResponse(1L, "admin", "Demo Admin", "admin@demo-shop.local", "ACTIVE")),
                2,
                5,
                11,
                3
        );

        when(userQueryService.pageUsers(eq(9L), any(UserPageQuery.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/users")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_READ")
                        .queryParam("page", "2")
                        .queryParam("size", "5")
                        .queryParam("username", "adm")
                        .queryParam("status", "ACTIVE")
                        .queryParam("roleCode", "TENANT_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.total").value(11))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.items[0].username").value("admin"));

        ArgumentCaptor<UserPageQuery> queryCaptor = ArgumentCaptor.forClass(UserPageQuery.class);
        verify(userQueryService).pageUsers(eq(9L), queryCaptor.capture());

        assertThat(queryCaptor.getValue().getPage()).isEqualTo(2);
        assertThat(queryCaptor.getValue().getSize()).isEqualTo(5);
        assertThat(queryCaptor.getValue().getUsername()).isEqualTo("adm");
        assertThat(queryCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(queryCaptor.getValue().getRoleCode()).isEqualTo("TENANT_ADMIN");
    }

    @Test
    void getUserDetailShouldReturnForbiddenWhenUserLacksPermission() throws Exception {
        mockMvc.perform(get("/api/v1/users/8")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "ORDER_READ"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("permission denied"));
    }

    @Test
    void getUserDetailShouldReturnTenantScopedDetail() throws Exception {
        UserDetailResponse response = new UserDetailResponse(
                8L,
                9L,
                "viewer",
                "Viewer User",
                "viewer@demo-shop.local",
                "ACTIVE",
                List.of("READ_ONLY"),
                LocalDateTime.of(2026, 3, 10, 10, 0),
                LocalDateTime.of(2026, 3, 10, 10, 30)
        );

        when(userQueryService.getUserDetail(9L, 8L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/8")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_READ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(8))
                .andExpect(jsonPath("$.data.username").value("viewer"))
                .andExpect(jsonPath("$.data.roleCodes[0]").value("READ_ONLY"));

        verify(userQueryService).getUserDetail(9L, 8L);
    }

    @Test
    void createUserShouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content(validCreateUserRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("authentication required"));
    }

    @Test
    void createUserShouldReturnForbiddenWhenUserLacksPermission() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_READ")
                        .contentType("application/json")
                        .content(validCreateUserRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("permission denied"));
    }

    @Test
    void createUserShouldReturnUnauthorizedWhenTenantContextIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_AUTHORITIES, "USER_WRITE")
                        .contentType("application/json")
                        .content(validCreateUserRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("tenant context missing"));
    }

    @Test
    void createUserShouldValidateRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_WRITE")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "",
                                  "displayName": "Cashier User",
                                  "email": "cashier@demo-shop.local",
                                  "password": "123456",
                                  "roleCodes": ["READ_ONLY"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("username: username must not be blank"));
    }

    @Test
    void createUserShouldRejectPasswordWithLeadingOrTrailingWhitespace() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_WRITE")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "cashier",
                                  "displayName": "Cashier User",
                                  "email": "cashier@demo-shop.local",
                                  "password": " 123456 ",
                                  "roleCodes": ["READ_ONLY"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("password: password must not start or end with whitespace"));
    }

    @Test
    void createUserShouldBindRequestAndReturnCreatedUser() throws Exception {
        UserCreateResponse response = new UserCreateResponse(
                8L,
                9L,
                "cashier",
                "Cashier User",
                "cashier@demo-shop.local",
                "ACTIVE",
                List.of("READ_ONLY"),
                LocalDateTime.of(2026, 3, 10, 12, 0),
                LocalDateTime.of(2026, 3, 10, 12, 0)
        );

        when(userCommandService.createUser(eq(9L), any(UserCreateCommand.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/users")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_WRITE")
                        .contentType("application/json")
                        .content(validCreateUserRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(8))
                .andExpect(jsonPath("$.data.username").value("cashier"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.roleCodes[0]").value("READ_ONLY"));

        ArgumentCaptor<UserCreateCommand> commandCaptor = ArgumentCaptor.forClass(UserCreateCommand.class);
        verify(userCommandService).createUser(eq(9L), commandCaptor.capture());

        assertThat(commandCaptor.getValue().getUsername()).isEqualTo("cashier");
        assertThat(commandCaptor.getValue().getDisplayName()).isEqualTo("Cashier User");
        assertThat(commandCaptor.getValue().getEmail()).isEqualTo("cashier@demo-shop.local");
        assertThat(commandCaptor.getValue().getPassword()).isEqualTo("123456");
        assertThat(commandCaptor.getValue().getRoleCodes()).containsExactly("READ_ONLY");
    }

    @Test
    void updateUserShouldValidateRequestBody() throws Exception {
        mockMvc.perform(put("/api/v1/users/8")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_WRITE")
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayName": "",
                                  "email": "updated@demo-shop.local"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("displayName: displayName must not be blank"));
    }

    @Test
    void updateUserShouldBindRequestAndReturnUpdatedUser() throws Exception {
        UserWriteResponse response = new UserWriteResponse(
                8L,
                9L,
                "cashier",
                "Updated Cashier",
                "updated@demo-shop.local",
                "ACTIVE",
                LocalDateTime.of(2026, 3, 10, 13, 0)
        );
        when(userCommandService.updateUser(eq(9L), eq(8L), any(UserUpdateCommand.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/8")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_WRITE")
                        .contentType("application/json")
                        .content(validUpdateUserRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(8))
                .andExpect(jsonPath("$.data.displayName").value("Updated Cashier"))
                .andExpect(jsonPath("$.data.email").value("updated@demo-shop.local"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        ArgumentCaptor<UserUpdateCommand> commandCaptor = ArgumentCaptor.forClass(UserUpdateCommand.class);
        verify(userCommandService).updateUser(eq(9L), eq(8L), commandCaptor.capture());
        assertThat(commandCaptor.getValue().getDisplayName()).isEqualTo("Updated Cashier");
        assertThat(commandCaptor.getValue().getEmail()).isEqualTo("updated@demo-shop.local");
    }

    @Test
    void updateUserStatusShouldRejectInvalidStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/users/8/status")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_WRITE")
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "ARCHIVED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("status: status must be one of ACTIVE, DISABLED"));
    }

    @Test
    void updateUserStatusShouldBindRequestAndReturnUpdatedUser() throws Exception {
        UserWriteResponse response = new UserWriteResponse(
                8L,
                9L,
                "cashier",
                "Updated Cashier",
                "updated@demo-shop.local",
                "DISABLED",
                LocalDateTime.of(2026, 3, 10, 14, 0)
        );
        when(userCommandService.updateStatus(eq(9L), eq(8L), any(UserStatusUpdateCommand.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/users/8/status")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_WRITE")
                        .contentType("application/json")
                        .content(validStatusUpdateRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(8))
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        ArgumentCaptor<UserStatusUpdateCommand> commandCaptor = ArgumentCaptor.forClass(UserStatusUpdateCommand.class);
        verify(userCommandService).updateStatus(eq(9L), eq(8L), commandCaptor.capture());
        assertThat(commandCaptor.getValue().getStatus()).isEqualTo("DISABLED");
    }

    @Test
    void assignRolesShouldValidateRequestBody() throws Exception {
        mockMvc.perform(put("/api/v1/users/8/roles")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_WRITE")
                        .contentType("application/json")
                        .content("""
                                {
                                  "roleCodes": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("roleCodes: roleCodes must not be empty"));
    }

    @Test
    void assignRolesShouldBindRequestAndReturnUpdatedRoles() throws Exception {
        UserRoleAssignmentResponse response = new UserRoleAssignmentResponse(
                8L,
                9L,
                "viewer",
                List.of("TENANT_ADMIN"),
                List.of("USER_READ", "USER_WRITE"),
                LocalDateTime.of(2026, 3, 10, 18, 0)
        );
        when(userCommandService.assignRoles(eq(9L), eq(8L), any(UserRoleAssignmentCommand.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/8/roles")
                        .header(HEADER_AUTH, "true")
                        .header(HEADER_TENANT_ID, "9")
                        .header(HEADER_TENANT_CODE, "demo-shop")
                        .header(HEADER_AUTHORITIES, "USER_WRITE")
                        .contentType("application/json")
                        .content(validAssignRolesRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(8))
                .andExpect(jsonPath("$.data.roleCodes[0]").value("TENANT_ADMIN"))
                .andExpect(jsonPath("$.data.permissionCodes[0]").value("USER_READ"));

        ArgumentCaptor<UserRoleAssignmentCommand> commandCaptor = ArgumentCaptor.forClass(UserRoleAssignmentCommand.class);
        verify(userCommandService).assignRoles(eq(9L), eq(8L), commandCaptor.capture());
        assertThat(commandCaptor.getValue().getRoleCodes()).containsExactly("TENANT_ADMIN");
    }

    private String validCreateUserRequest() {
        return """
                {
                  "username": "cashier",
                  "displayName": "Cashier User",
                  "email": "cashier@demo-shop.local",
                  "password": "123456",
                  "roleCodes": ["READ_ONLY"]
                }
                """;
    }

    private String validUpdateUserRequest() {
        return """
                {
                  "displayName": "Updated Cashier",
                  "email": "updated@demo-shop.local"
                }
                """;
    }

    private String validStatusUpdateRequest() {
        return """
                {
                  "status": "DISABLED"
                }
                """;
    }

    private String validAssignRolesRequest() {
        return """
                {
                  "roleCodes": ["TENANT_ADMIN"]
                }
                """;
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
