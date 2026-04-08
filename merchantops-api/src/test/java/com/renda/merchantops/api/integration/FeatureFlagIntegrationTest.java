package com.renda.merchantops.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.MerchantOpsApplication;
import com.renda.merchantops.api.dto.featureflag.command.FeatureFlagUpdateRequest;
import com.renda.merchantops.api.dto.featureflag.query.FeatureFlagItemResponse;
import com.renda.merchantops.api.featureflag.FeatureFlagCommandService;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = MerchantOpsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:featureflags;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
class FeatureFlagIntegrationTest {

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
    private FeatureFlagCommandService featureFlagCommandService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUpSchemaAndData() {
        assertThat(currentJdbcUrl()).contains("jdbc:h2:mem:featureflags");
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
                    created_at TIMESTAMP NOT NULL
                )
                """);
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

        seedTenants();
        seedPermissions();
        seedRoles();
        seedUsers();
        seedUserRoles();
        seedRolePermissions();
        seedFeatureFlags();
    }

    @Test
    void listFeatureFlagsShouldReturnStableKeyOrderForAuthorizedAdmin() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");

        mockMvc.perform(get("/api/v1/feature-flags")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items.length()").value(8))
                .andExpect(jsonPath("$.data.items[*].key", contains(
                        "ai.import.error-summary.enabled",
                        "ai.import.fix-recommendation.enabled",
                        "ai.import.mapping-suggestion.enabled",
                        "ai.ticket.reply-draft.enabled",
                        "ai.ticket.summary.enabled",
                        "ai.ticket.triage.enabled",
                        "workflow.import.selective-replay-proposal.enabled",
                        "workflow.ticket.comment-proposal.enabled"
                )));
    }

    @Test
    void listFeatureFlagsShouldReturnForbiddenWhenPermissionMissing() throws Exception {
        String viewerToken = loginAndGetToken("demo-shop", "viewer", "123456");

        mockMvc.perform(get("/api/v1/feature-flags")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(viewerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("permission denied"));
    }

    @Test
    void updateFeatureFlagShouldPersistStateAndWriteAuditSnapshot() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");

        mockMvc.perform(put("/api/v1/feature-flags/ai.ticket.summary.enabled")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header("X-Request-Id", "feature-flag-update-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.key").value("ai.ticket.summary.enabled"))
                .andExpect(jsonPath("$.data.enabled").value(false));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT enabled FROM feature_flag WHERE tenant_id = ? AND flag_key = ?",
                Boolean.class,
                1L,
                "ai.ticket.summary.enabled"
        )).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT enabled FROM feature_flag WHERE tenant_id = ? AND flag_key = ?",
                Boolean.class,
                2L,
                "ai.ticket.summary.enabled"
        )).isTrue();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE tenant_id = ? AND entity_type = 'FEATURE_FLAG' AND entity_id = ? AND action_type = 'FEATURE_FLAG_UPDATED'",
                Integer.class,
                1L,
                1L
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT before_value FROM audit_event WHERE tenant_id = ? AND entity_type = 'FEATURE_FLAG' AND entity_id = ?",
                String.class,
                1L,
                1L
        )).contains("\"tenantId\":1").contains("\"enabled\":true");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT after_value FROM audit_event WHERE tenant_id = ? AND entity_type = 'FEATURE_FLAG' AND entity_id = ?",
                String.class,
                1L,
                1L
        )).contains("\"tenantId\":1").contains("\"enabled\":false");
    }

    @Test
    void updateFeatureFlagShouldNotLeakAcrossTenants() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");
        String otherAdminToken = loginAndGetToken("other-shop", "otheradmin", "123456");

        mockMvc.perform(put("/api/v1/feature-flags/ai.ticket.summary.enabled")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header("X-Request-Id", "feature-flag-update-tenant-scope-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.key").value("ai.ticket.summary.enabled"))
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(get("/api/v1/feature-flags")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(otherAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[4].key").value("ai.ticket.summary.enabled"))
                .andExpect(jsonPath("$.data.items[4].enabled").value(true));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE tenant_id = ? AND entity_type = 'FEATURE_FLAG'",
                Integer.class,
                2L
        )).isZero();
    }

    @Test
    void updateFeatureFlagShouldReturnNotFoundForUnknownKey() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");

        mockMvc.perform(put("/api/v1/feature-flags/unknown.flag.enabled")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header("X-Request-Id", "feature-flag-missing-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":false}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("feature flag not found"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_event", Integer.class)).isZero();
    }

    @Test
    void updateFeatureFlagShouldBeIdempotentWhenEnabledValueUnchanged() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");
        Timestamp beforeUpdatedAt = jdbcTemplate.queryForObject(
                "SELECT updated_at FROM feature_flag WHERE tenant_id = ? AND flag_key = ?",
                Timestamp.class,
                1L,
                "ai.ticket.summary.enabled"
        );

        MvcResult result = mockMvc.perform(put("/api/v1/feature-flags/ai.ticket.summary.enabled")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header("X-Request-Id", "feature-flag-idempotent-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.key").value("ai.ticket.summary.enabled"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertThat(LocalDateTime.parse(root.path("data").path("updatedAt").asText()))
                .isEqualTo(beforeUpdatedAt.toLocalDateTime());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT updated_at FROM feature_flag WHERE tenant_id = ? AND flag_key = ?",
                Timestamp.class,
                1L,
                "ai.ticket.summary.enabled"
        )).isEqualTo(beforeUpdatedAt);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_event", Integer.class)).isZero();
    }

    @Test
    void updateFeatureFlagShouldRejectNullEnabledEvenWhenCurrentFlagAlreadyDisabled() throws Exception {
        String adminToken = loginAndGetToken("demo-shop", "admin", "123456");
        jdbcTemplate.update("""
                UPDATE feature_flag
                SET enabled = false, updated_by = ?, updated_at = CURRENT_TIMESTAMP
                WHERE tenant_id = ? AND flag_key = ?
                """, 101L, 1L, "ai.ticket.summary.enabled");

        mockMvc.perform(put("/api/v1/feature-flags/ai.ticket.summary.enabled")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminToken))
                        .header("X-Request-Id", "feature-flag-null-enabled-disabled-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("enabled must not be null"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT enabled FROM feature_flag WHERE tenant_id = ? AND flag_key = ?",
                Boolean.class,
                1L,
                "ai.ticket.summary.enabled"
        )).isFalse();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_event", Integer.class)).isZero();
    }

    @Test
    void updateFeatureFlagShouldSuppressDuplicateAuditWhenConcurrentWriterAlreadyAppliedTargetState() throws Exception {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstCompletedInsideTransaction = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch allowFirstCommit = new CountDownLatch(1);

        try {
            Future<FeatureFlagItemResponse> firstFuture = executor.submit(() -> transactionTemplate.execute(status -> {
                FeatureFlagItemResponse response = featureFlagCommandService.updateFlag(
                        1L,
                        101L,
                        "feature-flag-concurrent-1",
                        "ai.ticket.summary.enabled",
                        new FeatureFlagUpdateRequest(false)
                );
                firstCompletedInsideTransaction.countDown();
                awaitLatch(allowFirstCommit, "first transaction should be released for commit");
                return response;
            }));

            assertThat(firstCompletedInsideTransaction.await(5, TimeUnit.SECONDS)).isTrue();

            Future<FeatureFlagItemResponse> secondFuture = executor.submit(() -> transactionTemplate.execute(status -> {
                secondStarted.countDown();
                return featureFlagCommandService.updateFlag(
                        1L,
                        103L,
                        "feature-flag-concurrent-2",
                        "ai.ticket.summary.enabled",
                        new FeatureFlagUpdateRequest(false)
                );
            }));

            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> secondFuture.get(500, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            allowFirstCommit.countDown();

            FeatureFlagItemResponse firstResponse = firstFuture.get(10, TimeUnit.SECONDS);
            FeatureFlagItemResponse secondResponse = secondFuture.get(10, TimeUnit.SECONDS);

            assertThat(firstResponse.key()).isEqualTo("ai.ticket.summary.enabled");
            assertThat(firstResponse.enabled()).isFalse();
            assertThat(secondResponse.key()).isEqualTo("ai.ticket.summary.enabled");
            assertThat(secondResponse.enabled()).isFalse();
            Timestamp finalUpdatedAt = jdbcTemplate.queryForObject(
                    "SELECT updated_at FROM feature_flag WHERE tenant_id = ? AND flag_key = ?",
                    Timestamp.class,
                    1L,
                    "ai.ticket.summary.enabled"
            );
            assertThat(finalUpdatedAt).isNotNull();
            assertThat(Math.abs(ChronoUnit.MICROS.between(finalUpdatedAt.toLocalDateTime(), secondResponse.updatedAt())))
                    .isLessThanOrEqualTo(1L);

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT enabled FROM feature_flag WHERE tenant_id = ? AND flag_key = ?",
                    Boolean.class,
                    1L,
                    "ai.ticket.summary.enabled"
            )).isFalse();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT updated_by FROM feature_flag WHERE tenant_id = ? AND flag_key = ?",
                    Long.class,
                    1L,
                    "ai.ticket.summary.enabled"
            )).isEqualTo(101L);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM audit_event WHERE tenant_id = ? AND entity_type = 'FEATURE_FLAG' AND action_type = 'FEATURE_FLAG_UPDATED'",
                    Integer.class,
                    1L
            )).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT request_id FROM audit_event WHERE tenant_id = ? AND entity_type = 'FEATURE_FLAG' AND action_type = 'FEATURE_FLAG_UPDATED'",
                    String.class,
                    1L
            )).isEqualTo("feature-flag-concurrent-1");
        } finally {
            allowFirstCommit.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void updateFeatureFlagShouldApplySecondConcurrentRequestWhenItTargetsOppositeState() throws Exception {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstCompletedInsideTransaction = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch allowFirstCommit = new CountDownLatch(1);

        try {
            Future<FeatureFlagItemResponse> firstFuture = executor.submit(() -> transactionTemplate.execute(status -> {
                FeatureFlagItemResponse response = featureFlagCommandService.updateFlag(
                        1L,
                        101L,
                        "feature-flag-concurrent-opposite-1",
                        "ai.ticket.summary.enabled",
                        new FeatureFlagUpdateRequest(false)
                );
                firstCompletedInsideTransaction.countDown();
                awaitLatch(allowFirstCommit, "first transaction should be released for commit");
                return response;
            }));

            assertThat(firstCompletedInsideTransaction.await(5, TimeUnit.SECONDS)).isTrue();

            Future<FeatureFlagItemResponse> secondFuture = executor.submit(() -> transactionTemplate.execute(status -> {
                secondStarted.countDown();
                return featureFlagCommandService.updateFlag(
                        1L,
                        103L,
                        "feature-flag-concurrent-opposite-2",
                        "ai.ticket.summary.enabled",
                        new FeatureFlagUpdateRequest(true)
                );
            }));

            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> secondFuture.get(500, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            allowFirstCommit.countDown();

            FeatureFlagItemResponse firstResponse = firstFuture.get(10, TimeUnit.SECONDS);
            FeatureFlagItemResponse secondResponse = secondFuture.get(10, TimeUnit.SECONDS);

            assertThat(firstResponse.key()).isEqualTo("ai.ticket.summary.enabled");
            assertThat(firstResponse.enabled()).isFalse();
            assertThat(secondResponse.key()).isEqualTo("ai.ticket.summary.enabled");
            assertThat(secondResponse.enabled()).isTrue();
            Timestamp finalUpdatedAt = jdbcTemplate.queryForObject(
                    "SELECT updated_at FROM feature_flag WHERE tenant_id = ? AND flag_key = ?",
                    Timestamp.class,
                    1L,
                    "ai.ticket.summary.enabled"
            );
            assertThat(finalUpdatedAt).isNotNull();
            assertThat(Math.abs(ChronoUnit.MICROS.between(finalUpdatedAt.toLocalDateTime(), secondResponse.updatedAt())))
                    .isLessThanOrEqualTo(1L);

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT enabled FROM feature_flag WHERE tenant_id = ? AND flag_key = ?",
                    Boolean.class,
                    1L,
                    "ai.ticket.summary.enabled"
            )).isTrue();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT updated_by FROM feature_flag WHERE tenant_id = ? AND flag_key = ?",
                    Long.class,
                    1L,
                    "ai.ticket.summary.enabled"
            )).isEqualTo(103L);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM audit_event WHERE tenant_id = ? AND entity_type = 'FEATURE_FLAG' AND action_type = 'FEATURE_FLAG_UPDATED'",
                    Integer.class,
                    1L
            )).isEqualTo(2);
            assertThat(jdbcTemplate.queryForList(
                    "SELECT request_id FROM audit_event WHERE tenant_id = ? AND entity_type = 'FEATURE_FLAG' AND action_type = 'FEATURE_FLAG_UPDATED' ORDER BY id",
                    String.class,
                    1L
            )).containsExactly(
                    "feature-flag-concurrent-opposite-1",
                    "feature-flag-concurrent-opposite-2"
            );
        } finally {
            allowFirstCommit.countDown();
            executor.shutdownNow();
        }
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
        insertPermission(5L, "FEATURE_FLAG_MANAGE", "Manage feature flag");
    }

    private void seedRoles() {
        insertRole(11L, 1L, "TENANT_ADMIN", "Tenant Admin");
        insertRole(13L, 1L, "READ_ONLY", "Read Only User");
        insertRole(21L, 2L, "TENANT_ADMIN", "Tenant Admin");
    }

    private void seedUsers() {
        String encodedPassword = passwordEncoder.encode("123456");
        insertUser(101L, 1L, "admin", encodedPassword, "Demo Admin", "admin@demo-shop.local", "ACTIVE");
        insertUser(103L, 1L, "viewer", encodedPassword, "Viewer User", "viewer@demo-shop.local", "ACTIVE");
        insertUser(201L, 2L, "otheradmin", encodedPassword, "Other Admin", "otheradmin@other-shop.local", "ACTIVE");
    }

    private void seedUserRoles() {
        insertUserRole(1001L, 101L, 11L);
        insertUserRole(1003L, 103L, 13L);
        insertUserRole(2001L, 201L, 21L);
    }

    private void seedRolePermissions() {
        insertRolePermission(2001L, 11L, 1L);
        insertRolePermission(2002L, 11L, 2L);
        insertRolePermission(2003L, 11L, 5L);
        insertRolePermission(2004L, 13L, 1L);
        insertRolePermission(3001L, 21L, 1L);
        insertRolePermission(3002L, 21L, 2L);
        insertRolePermission(3003L, 21L, 5L);
    }

    private void seedFeatureFlags() {
        List.of(
                "ai.ticket.summary.enabled",
                "ai.ticket.triage.enabled",
                "ai.ticket.reply-draft.enabled",
                "ai.import.error-summary.enabled",
                "ai.import.mapping-suggestion.enabled",
                "ai.import.fix-recommendation.enabled",
                "workflow.import.selective-replay-proposal.enabled",
                "workflow.ticket.comment-proposal.enabled"
        ).stream().forEachOrdered(key -> {
            insertFeatureFlag((long) (featureFlagOrder(key) + 1), key, true);
            insertFeatureFlag(2L, (long) (featureFlagOrder(key) + 101), key, true);
        });
    }

    private int featureFlagOrder(String key) {
        return List.of(
                "ai.ticket.summary.enabled",
                "ai.ticket.triage.enabled",
                "ai.ticket.reply-draft.enabled",
                "ai.import.error-summary.enabled",
                "ai.import.mapping-suggestion.enabled",
                "ai.import.fix-recommendation.enabled",
                "workflow.import.selective-replay-proposal.enabled",
                "workflow.ticket.comment-proposal.enabled"
        ).indexOf(key);
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

    private void insertFeatureFlag(Long id, String key, boolean enabled) {
        insertFeatureFlag(1L, id, key, enabled);
    }

    private void insertFeatureFlag(Long tenantId, Long id, String key, boolean enabled) {
        jdbcTemplate.update("""
                INSERT INTO feature_flag (id, tenant_id, flag_key, enabled, updated_by, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, id, tenantId, key, enabled, tenantId == 1L ? 101L : 201L);
    }

    private String loginAndGetToken(String tenantCode, String username, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(tenantCode, username, password)))
                .andExpect(status().isOk())
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

    private void awaitLatch(CountDownLatch latch, String message) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).as(message).isTrue();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(message, ex);
        }
    }
}
