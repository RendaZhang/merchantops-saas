package com.renda.merchantops.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.MerchantOpsApplication;
import com.renda.merchantops.api.security.CurrentUser;
import com.renda.merchantops.api.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
                "spring.datasource.url=jdbc:h2:mem:authsecurity;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
class AuthSecurityIntegrationTest {

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

    @Autowired
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUpSchemaAndData() {
        assertThat(currentJdbcUrl()).contains("jdbc:h2:mem:authsecurity");
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
                    updated_at TIMESTAMP NOT NULL
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

        seedTenants();
        seedPermissions();
        seedRoles();
        seedUsers();
        seedUserRoles();
        seedRolePermissions();
    }

    @Test
    void loginShouldIssueJwtWithTenantRolesAndPermissions() throws Exception {
        String token = loginAndGetToken("demo-shop", "admin", "123456");

        CurrentUser currentUser = jwtTokenService.parseCurrentUser(token);

        assertThat(currentUser.getUserId()).isEqualTo(101L);
        assertThat(currentUser.getTenantId()).isEqualTo(1L);
        assertThat(currentUser.getTenantCode()).isEqualTo("demo-shop");
        assertThat(currentUser.getUsername()).isEqualTo("admin");
        assertThat(currentUser.getRoles()).containsExactly("TENANT_ADMIN");
        assertThat(currentUser.getPermissions())
                .containsExactly("USER_READ", "USER_WRITE", "ORDER_READ", "BILLING_READ", "FEATURE_FLAG_MANAGE");
    }

    @Test
    void loginShouldRejectWrongPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest("demo-shop", "admin", "wrong-password")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("username or password is incorrect"));
    }

    @Test
    void loginShouldRejectPasswordWithLeadingOrTrailingWhitespace() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest("demo-shop", "admin", " 123456 ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("password: password must not start or end with whitespace"));
    }

    @Test
    void listUsersShouldReturnUnauthorizedWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("authentication required"));
    }

    @Test
    void listUsersShouldReturnUnauthorizedWhenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("authentication required"));
    }

    @Test
    void listUsersShouldReturnForbiddenWhenPermissionIsMissing() throws Exception {
        String token = loginAndGetToken("demo-shop", "billing", "123456");

        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("permission denied"));
    }

    @Test
    void listUsersShouldReturnTenantScopedUsersWhenPermissionIsGranted() throws Exception {
        String token = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .queryParam("page", "0")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.items[*].username", contains("admin", "ops", "viewer", "billing")))
                .andExpect(jsonPath("$.data.items[*].username", not(hasItem("outsider"))))
                .andExpect(jsonPath("$.data.items[0].passwordHash").doesNotExist());
    }

    @Test
    void listRolesShouldReturnForbiddenWhenPermissionIsMissing() throws Exception {
        String token = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/roles")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("permission denied"));
    }

    @Test
    void listRolesShouldReturnTenantScopedRolesWhenPermissionIsGranted() throws Exception {
        String token = loginAndGetToken("demo-shop", "admin", "123456");

        mockMvc.perform(get("/api/v1/roles")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[*].roleCode", contains("TENANT_ADMIN", "OPS_USER", "READ_ONLY", "BILLING_USER")))
                .andExpect(jsonPath("$.data.items[*].roleCode", not(hasItem("OTHER_ONLY"))));
    }

    @Test
    void createUserShouldReturnForbiddenWhenPermissionIsMissing() throws Exception {
        String token = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserRequest("cashier", "Cashier User", "cashier@demo-shop.local", "123456", "[\"READ_ONLY\"]")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("permission denied"));
    }

    @Test
    void updateUserShouldReturnForbiddenWhenPermissionIsMissing() throws Exception {
        String token = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(put("/api/v1/users/103")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateUserRequest("Viewer Updated", "viewer.updated@demo-shop.local")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("permission denied"));
    }

    @Test
    void createUserShouldRejectRoleCodeOutsideCurrentTenant() throws Exception {
        String token = loginAndGetToken("demo-shop", "admin", "123456");

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserRequest("cashier", "Cashier User", "cashier@demo-shop.local", "123456", "[\"OTHER_ONLY\"]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("roleCodes must exist in current tenant"));
    }

    @Test
    void updateUserShouldPersistProfileWithinTenantWhenPermissionIsGranted() throws Exception {
        String token = loginAndGetToken("demo-shop", "admin", "123456");

        mockMvc.perform(put("/api/v1/users/103")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateUserRequest("Viewer Updated", "viewer.updated@demo-shop.local")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(103))
                .andExpect(jsonPath("$.data.username").value("viewer"))
                .andExpect(jsonPath("$.data.displayName").value("Viewer Updated"))
                .andExpect(jsonPath("$.data.email").value("viewer.updated@demo-shop.local"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT display_name FROM users WHERE id = ?",
                String.class,
                103L
        )).isEqualTo("Viewer Updated");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT email FROM users WHERE id = ?",
                String.class,
                103L
        )).isEqualTo("viewer.updated@demo-shop.local");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT tenant_id FROM users WHERE id = ?",
                Long.class,
                103L
        )).isEqualTo(1L);
    }

    @Test
    void updateUserStatusShouldDisableUserAndRejectLogin() throws Exception {
        String token = loginAndGetToken("demo-shop", "admin", "123456");

        mockMvc.perform(patch("/api/v1/users/103/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusUpdateRequest("DISABLED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(103))
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM users WHERE id = ?",
                String.class,
                103L
        )).isEqualTo("DISABLED");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest("demo-shop", "viewer", "123456")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("user is not active"));
    }

    @Test
    void disabledUserOldTokenShouldBeRejectedOnProtectedEndpoint() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");

        mockMvc.perform(patch("/api/v1/users/103/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusUpdateRequest("DISABLED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("page", "0")
                        .queryParam("size", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("user is not active"));
    }

    @Test
    void assignRolesShouldReplaceRolesInvalidateOldTokenAndGrantNewPermissionsAfterRelogin() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");

        mockMvc.perform(put("/api/v1/users/103/roles")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignRolesRequest("[\"TENANT_ADMIN\"]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(103))
                .andExpect(jsonPath("$.data.roleCodes[0]").value("TENANT_ADMIN"))
                .andExpect(jsonPath("$.data.permissionCodes", hasItem("FEATURE_FLAG_MANAGE")));

        assertThat(jdbcTemplate.queryForList("""
                        SELECT r.role_code
                        FROM user_role ur
                        JOIN `role` r ON r.id = ur.role_id
                        WHERE ur.user_id = ?
                        ORDER BY r.id
                        """, String.class, 103L))
                .containsExactly("TENANT_ADMIN");

        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken))
                        .queryParam("page", "0")
                        .queryParam("size", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("token claims are stale, please login again"));

        String refreshedViewerToken = loginAndGetToken("demo-shop", "viewer", "123456");
        CurrentUser refreshedViewer = jwtTokenService.parseCurrentUser(refreshedViewerToken);
        assertThat(refreshedViewer.getRoles()).containsExactly("TENANT_ADMIN");
        assertThat(refreshedViewer.getPermissions())
                .containsExactly("USER_READ", "USER_WRITE", "ORDER_READ", "BILLING_READ", "FEATURE_FLAG_MANAGE");

        mockMvc.perform(get("/api/v1/rbac/users/manage")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(refreshedViewerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(get("/api/v1/rbac/feature-flags")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(refreshedViewerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void updateUserStatusShouldRejectInvalidStatus() throws Exception {
        String token = loginAndGetToken("demo-shop", "admin", "123456");

        mockMvc.perform(patch("/api/v1/users/103/status")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusUpdateRequest("ARCHIVED")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("status: status must be one of ACTIVE, DISABLED"));
    }

    @Test
    void createUserShouldRejectPasswordWithLeadingOrTrailingWhitespace() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserRequest("cashier", "Cashier User", "cashier@demo-shop.local", " 123456 ", "[\"READ_ONLY\"]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("password: password must not start or end with whitespace"));
    }

    @Test
    void createUserShouldPersistTenantScopedUserAndAllowLogin() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserRequest("cashier", "Cashier User", "cashier@demo-shop.local", "123456", "[\"READ_ONLY\"]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.username").value("cashier"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.roleCodes", hasSize(1)))
                .andExpect(jsonPath("$.data.roleCodes[0]").value("READ_ONLY"));

        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE tenant_id = ? AND username = ?",
                Long.class,
                1L,
                "cashier"
        );
        assertThat(userId).isNotNull();

        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE id = ?",
                String.class,
                userId
        );
        assertThat(passwordHash).isNotBlank();
        assertThat(passwordHash).isNotEqualTo("123456");
        assertThat(passwordEncoder.matches("123456", passwordHash)).isTrue();

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM users WHERE id = ?",
                String.class,
                userId
        );
        assertThat(status).isEqualTo("ACTIVE");

        assertThat(jdbcTemplate.queryForList("""
                        SELECT r.role_code
                        FROM user_role ur
                        JOIN `role` r ON r.id = ur.role_id
                        WHERE ur.user_id = ?
                        ORDER BY r.id
                        """, String.class, userId))
                .containsExactly("READ_ONLY");

        String createdUserToken = loginAndGetToken("demo-shop", "cashier", "123456");
        CurrentUser currentUser = jwtTokenService.parseCurrentUser(createdUserToken);
        assertThat(currentUser.getTenantId()).isEqualTo(1L);
        assertThat(currentUser.getUsername()).isEqualTo("cashier");
        assertThat(currentUser.getRoles()).containsExactly("READ_ONLY");
        assertThat(currentUser.getPermissions()).containsExactly("USER_READ");
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
    }

    private void seedRoles() {
        insertRole(11L, 1L, "TENANT_ADMIN", "Tenant Admin");
        insertRole(12L, 1L, "OPS_USER", "Operations User");
        insertRole(13L, 1L, "READ_ONLY", "Read Only User");
        insertRole(14L, 1L, "BILLING_USER", "Billing User");
        insertRole(21L, 2L, "TENANT_ADMIN", "Tenant Admin");
        insertRole(22L, 2L, "OTHER_ONLY", "Other Tenant Role");
    }

    private void seedUsers() {
        String encodedPassword = passwordEncoder.encode("123456");

        insertUser(101L, 1L, "admin", encodedPassword, "Demo Admin", "admin@demo-shop.local", "ACTIVE");
        insertUser(102L, 1L, "ops", encodedPassword, "Ops User", "ops@demo-shop.local", "ACTIVE");
        insertUser(103L, 1L, "viewer", encodedPassword, "Viewer User", "viewer@demo-shop.local", "ACTIVE");
        insertUser(104L, 1L, "billing", encodedPassword, "Billing User", "billing@demo-shop.local", "ACTIVE");
        insertUser(201L, 2L, "outsider", encodedPassword, "Other Tenant User", "outsider@other-shop.local", "ACTIVE");
    }

    private void seedUserRoles() {
        insertUserRole(1001L, 101L, 11L);
        insertUserRole(1002L, 102L, 12L);
        insertUserRole(1003L, 103L, 13L);
        insertUserRole(1004L, 104L, 14L);
        insertUserRole(1005L, 201L, 21L);
    }

    private void seedRolePermissions() {
        insertRolePermission(2001L, 11L, 1L);
        insertRolePermission(2002L, 11L, 2L);
        insertRolePermission(2003L, 11L, 3L);
        insertRolePermission(2004L, 11L, 4L);
        insertRolePermission(2005L, 11L, 5L);
        insertRolePermission(2006L, 12L, 1L);
        insertRolePermission(2007L, 12L, 3L);
        insertRolePermission(2008L, 13L, 1L);
        insertRolePermission(2009L, 14L, 4L);
        insertRolePermission(2010L, 21L, 1L);
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

    private String loginAndGetToken(String tenantCode, String username, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(tenantCode, username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(7200))
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

    private String createUserRequest(String username,
                                     String displayName,
                                     String email,
                                     String password,
                                     String roleCodesJson) {
        return """
                {
                  "username": "%s",
                  "displayName": "%s",
                  "email": "%s",
                  "password": "%s",
                  "roleCodes": %s
                }
                """.formatted(username, displayName, email, password, roleCodesJson);
    }

    private String updateUserRequest(String displayName, String email) {
        return """
                {
                  "displayName": "%s",
                  "email": "%s"
                }
                """.formatted(displayName, email);
    }

    private String statusUpdateRequest(String status) {
        return """
                {
                  "status": "%s"
                }
                """.formatted(status);
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
