package com.renda.merchantops.api.integration;

import org.h2.Driver;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketChildTableTenantLinkageMigrationTest {

    @Test
    void v20ShouldRejectTicketChildRowsLinkedToDifferentTenant() throws Exception {
        String url = "jdbc:h2:mem:ticket-child-link-migration-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource(new Driver(), url, "sa", "");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        createPreV20Schema(jdbcTemplate);
        seedTenantUsersAndTickets(jdbcTemplate);
        insertComment(jdbcTemplate, 5101L, 1L, 301L, 101L, "pre-v20-comment-valid");
        insertOperationLog(jdbcTemplate, 5201L, 1L, 301L, 101L, "pre-v20-log-valid");

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V20__enforce_ticket_child_table_tenant_linkage.sql"));
        }

        insertComment(jdbcTemplate, 5102L, 1L, 301L, 102L, "post-v20-comment-valid");
        insertOperationLog(jdbcTemplate, 5202L, 1L, 301L, 102L, "post-v20-log-valid");

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket_comment", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket_operation_log", Integer.class)).isEqualTo(2);

        assertThatThrownBy(() -> insertComment(jdbcTemplate, 5103L, 2L, 301L, 201L, "post-v20-comment-cross-tenant-ticket"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertOperationLog(jdbcTemplate, 5203L, 2L, 301L, 201L, "post-v20-log-cross-tenant-ticket"))
                .isInstanceOf(DataAccessException.class);
    }

    private void createPreV20Schema(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE tenant (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE users (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    tenant_id BIGINT NOT NULL,
                    CONSTRAINT uk_users_id_tenant UNIQUE (id, tenant_id),
                    CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE ticket (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    tenant_id BIGINT NOT NULL,
                    created_by BIGINT NOT NULL,
                    CONSTRAINT fk_ticket_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
                    CONSTRAINT fk_ticket_created_by FOREIGN KEY (created_by) REFERENCES users(id),
                    CONSTRAINT fk_ticket_created_by_tenant FOREIGN KEY (created_by, tenant_id) REFERENCES users(id, tenant_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE ticket_comment (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    tenant_id BIGINT NOT NULL,
                    ticket_id BIGINT NOT NULL,
                    content VARCHAR(2000) NOT NULL,
                    created_by BIGINT NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    CONSTRAINT fk_ticket_comment_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
                    CONSTRAINT fk_ticket_comment_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(id),
                    CONSTRAINT fk_ticket_comment_created_by FOREIGN KEY (created_by) REFERENCES users(id),
                    CONSTRAINT fk_ticket_comment_created_by_tenant FOREIGN KEY (created_by, tenant_id) REFERENCES users(id, tenant_id)
                )
                """);
        jdbcTemplate.execute("CREATE INDEX idx_ticket_comment_ticket ON ticket_comment (ticket_id, tenant_id, id)");
        jdbcTemplate.execute("CREATE INDEX idx_ticket_comment_created_by_tenant ON ticket_comment (created_by, tenant_id)");
        jdbcTemplate.execute("""
                CREATE TABLE ticket_operation_log (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    tenant_id BIGINT NOT NULL,
                    ticket_id BIGINT NOT NULL,
                    operation_type VARCHAR(64) NOT NULL,
                    detail VARCHAR(512) NOT NULL,
                    operator_id BIGINT NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    CONSTRAINT fk_ticket_operation_log_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
                    CONSTRAINT fk_ticket_operation_log_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(id),
                    CONSTRAINT fk_ticket_operation_log_operator FOREIGN KEY (operator_id) REFERENCES users(id),
                    CONSTRAINT fk_ticket_operation_log_operator_tenant FOREIGN KEY (operator_id, tenant_id) REFERENCES users(id, tenant_id)
                )
                """);
        jdbcTemplate.execute("CREATE INDEX idx_ticket_operation_log_ticket ON ticket_operation_log (ticket_id, tenant_id, id)");
        jdbcTemplate.execute("CREATE INDEX idx_ticket_operation_log_operator_tenant ON ticket_operation_log (operator_id, tenant_id)");
    }

    private void seedTenantUsersAndTickets(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("INSERT INTO tenant (id) VALUES (?)", 1L);
        jdbcTemplate.update("INSERT INTO tenant (id) VALUES (?)", 2L);
        jdbcTemplate.update("INSERT INTO users (id, tenant_id) VALUES (?, ?)", 101L, 1L);
        jdbcTemplate.update("INSERT INTO users (id, tenant_id) VALUES (?, ?)", 102L, 1L);
        jdbcTemplate.update("INSERT INTO users (id, tenant_id) VALUES (?, ?)", 201L, 2L);
        jdbcTemplate.update("INSERT INTO ticket (id, tenant_id, created_by) VALUES (?, ?, ?)", 301L, 1L, 101L);
        jdbcTemplate.update("INSERT INTO ticket (id, tenant_id, created_by) VALUES (?, ?, ?)", 401L, 2L, 201L);
    }

    private void insertComment(JdbcTemplate jdbcTemplate,
                               Long id,
                               Long tenantId,
                               Long ticketId,
                               Long createdBy,
                               String requestId) {
        jdbcTemplate.update("""
                INSERT INTO ticket_comment (id, tenant_id, ticket_id, content, created_by, request_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, id, tenantId, ticketId, "migration comment", createdBy, requestId);
    }

    private void insertOperationLog(JdbcTemplate jdbcTemplate,
                                    Long id,
                                    Long tenantId,
                                    Long ticketId,
                                    Long operatorId,
                                    String requestId) {
        jdbcTemplate.update("""
                INSERT INTO ticket_operation_log (id, tenant_id, ticket_id, operation_type, detail, operator_id, request_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, id, tenantId, ticketId, "COMMENTED", "comment added", operatorId, requestId);
    }
}
