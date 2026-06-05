package com.renda.merchantops.infra.repository;

import com.renda.merchantops.infra.persistence.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:userrepo;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void setUpSchemaAndData() {
        assertThat(currentJdbcUrl()).contains("jdbc:h2:mem:userrepo");
        assertThat(currentDatabaseMode()).isEqualTo("MySQL");

        jdbcTemplate.execute("DROP ALL OBJECTS");

        jdbcTemplate.execute("""
                CREATE TABLE tenant (
                    id BIGINT PRIMARY KEY,
                    tenant_code VARCHAR(64) NOT NULL,
                    tenant_name VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE users (
                    id BIGINT PRIMARY KEY,
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
                    id BIGINT PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    role_code VARCHAR(64) NOT NULL,
                    role_name VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL,
                    CONSTRAINT uk_role_id_tenant UNIQUE (id, tenant_id)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE user_role (
                    id BIGINT PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    role_id BIGINT NOT NULL,
                    CONSTRAINT fk_user_role_user_tenant FOREIGN KEY (user_id, tenant_id) REFERENCES users(id, tenant_id),
                    CONSTRAINT fk_user_role_role_tenant FOREIGN KEY (role_id, tenant_id) REFERENCES `role`(id, tenant_id)
                )
                """);

        jdbcTemplate.update("INSERT INTO tenant (id, tenant_code, tenant_name, status, created_at, updated_at) VALUES (1, 'demo-shop', 'Demo Shop', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO tenant (id, tenant_code, tenant_name, status, created_at, updated_at) VALUES (2, 'other-shop', 'Other Shop', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        jdbcTemplate.update("INSERT INTO users (id, tenant_id, username, password_hash, display_name, email, status, created_at, updated_at) VALUES (1, 1, 'admin', 'hash', 'Demo Admin', 'admin@demo-shop.local', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO users (id, tenant_id, username, password_hash, display_name, email, status, created_at, updated_at) VALUES (2, 1, 'ops', 'hash', 'Ops User', 'ops@demo-shop.local', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO users (id, tenant_id, username, password_hash, display_name, email, status, created_at, updated_at) VALUES (3, 1, 'viewer', 'hash', 'Viewer User', 'viewer@demo-shop.local', 'INACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO users (id, tenant_id, username, password_hash, display_name, email, status, created_at, updated_at) VALUES (4, 2, 'admin', 'hash', 'Other Admin', 'admin@other-shop.local', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        jdbcTemplate.update("INSERT INTO `role` (id, tenant_id, role_code, role_name, created_at, updated_at) VALUES (1, 1, 'TENANT_ADMIN', 'Tenant Admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO `role` (id, tenant_id, role_code, role_name, created_at, updated_at) VALUES (2, 1, 'OPS_USER', 'Ops User', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO `role` (id, tenant_id, role_code, role_name, created_at, updated_at) VALUES (3, 1, 'READ_ONLY', 'Read Only', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO `role` (id, tenant_id, role_code, role_name, created_at, updated_at) VALUES (4, 2, 'TENANT_ADMIN', 'Tenant Admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        jdbcTemplate.update("INSERT INTO user_role (id, tenant_id, user_id, role_id) VALUES (1, 1, 1, 1)");
        jdbcTemplate.update("INSERT INTO user_role (id, tenant_id, user_id, role_id) VALUES (2, 1, 1, 2)");
        jdbcTemplate.update("INSERT INTO user_role (id, tenant_id, user_id, role_id) VALUES (3, 1, 2, 2)");
        jdbcTemplate.update("INSERT INTO user_role (id, tenant_id, user_id, role_id) VALUES (4, 1, 3, 3)");
        jdbcTemplate.update("INSERT INTO user_role (id, tenant_id, user_id, role_id) VALUES (5, 2, 4, 4)");
    }

    @Test
    void searchPageByTenantIdShouldFilterByTenantUsernameStatusAndRole() {
        Page<UserEntity> result = userRepository.searchPageByTenantId(
                1L,
                "ad",
                "ACTIVE",
                "TENANT_ADMIN",
                PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getContent())
                .extracting(UserEntity::getUsername)
                .containsExactly("admin");
    }

    @Test
    void searchPageByTenantIdShouldDeduplicateUsersAndKeepPaginationStable() {
        Page<UserEntity> firstPage = userRepository.searchPageByTenantId(
                1L,
                null,
                null,
                null,
                PageRequest.of(0, 2)
        );

        Page<UserEntity> secondPage = userRepository.searchPageByTenantId(
                1L,
                null,
                null,
                null,
                PageRequest.of(1, 2)
        );

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent())
                .extracting(UserEntity::getUsername)
                .containsExactly("admin", "ops");
        assertThat(secondPage.getContent())
                .extracting(UserEntity::getUsername)
                .containsExactly("viewer");
        assertThat(StreamConcatHelper.usernames(firstPage.getContent(), secondPage.getContent()))
                .containsExactly("admin", "ops", "viewer");
    }

    private static final class StreamConcatHelper {

        private StreamConcatHelper() {
        }

        private static List<String> usernames(List<UserEntity> first, List<UserEntity> second) {
            return java.util.stream.Stream.concat(first.stream(), second.stream())
                    .map(UserEntity::getUsername)
                    .toList();
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

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    @EntityScan(basePackageClasses = UserEntity.class)
    static class TestJpaConfiguration {
    }
}
