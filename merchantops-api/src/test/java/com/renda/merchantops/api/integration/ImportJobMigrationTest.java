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

class ImportJobMigrationTest {

    @Test
    void v9ShouldRejectCrossTenantImportJobItemErrors() throws Exception {
        String url = "jdbc:h2:mem:importjob-migration-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource(new Driver(), url, "sa", "");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

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

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V9__add_import_job_backbone.sql"));
        }

        jdbcTemplate.update("INSERT INTO tenant (id) VALUES (?)", 1L);
        jdbcTemplate.update("INSERT INTO tenant (id) VALUES (?)", 2L);
        jdbcTemplate.update("INSERT INTO users (id, tenant_id) VALUES (?, ?)", 101L, 1L);
        jdbcTemplate.update("""
                INSERT INTO import_job (
                    tenant_id, import_type, source_type, source_filename, storage_key, status,
                    requested_by, request_id, total_count, success_count, failure_count
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, 1L, "USER_CSV", "CSV", "users.csv", "1/key.csv", "QUEUED", 101L, "req-1", 0, 0, 0);
        Long jobId = jdbcTemplate.queryForObject(
                "SELECT id FROM import_job WHERE tenant_id = ? AND request_id = ?",
                Long.class,
                1L,
                "req-1"
        );

        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (
                    tenant_id, import_job_id, source_row_number, error_code, error_message
                ) VALUES (?, ?, ?, ?, ?)
                """, 1L, jobId, 1, "INVALID_ROW_SHAPE", "column count mismatch");
        Integer validRowCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM import_job_item_error", Integer.class);
        assertThat(validRowCount).isEqualTo(1);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                        INSERT INTO import_job_item_error (
                            tenant_id, import_job_id, source_row_number, error_code, error_message
                        ) VALUES (?, ?, ?, ?, ?)
                        """, 2L, jobId, 2, "INVALID_ROW_SHAPE", "column count mismatch"))
                .isInstanceOf(DataAccessException.class);
    }
}
