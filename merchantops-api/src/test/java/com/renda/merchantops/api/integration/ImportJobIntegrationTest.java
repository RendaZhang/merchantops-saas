package com.renda.merchantops.api.integration;

import com.renda.merchantops.api.MerchantOpsApplication;
import com.renda.merchantops.api.dto.importjob.command.ImportJobCreateRequest;
import com.renda.merchantops.api.dto.importjob.query.ImportJobDetailResponse;
import com.renda.merchantops.api.messaging.ImportJobMessage;
import com.renda.merchantops.api.messaging.ImportJobPublisher;
import com.renda.merchantops.api.messaging.ImportJobWorker;
import com.renda.merchantops.api.service.ImportJobCommandService;
import com.renda.merchantops.api.service.ImportJobQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = MerchantOpsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:importjob;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driverClassName=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.sql.init.mode=never",
                "spring.flyway.enabled=false",
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "merchantops.import.storage.local-dir=target/test-imports"
        }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ImportJobIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ImportJobCommandService importJobCommandService;
    @Autowired
    private ImportJobQueryService importJobQueryService;
    @Autowired
    private ImportJobWorker importJobWorker;

    @MockBean
    private ImportJobPublisher importJobPublisher;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("DROP ALL OBJECTS");
        jdbcTemplate.execute("""
                CREATE TABLE tenant (id BIGINT PRIMARY KEY, tenant_code VARCHAR(64), tenant_name VARCHAR(128), status VARCHAR(32), created_at TIMESTAMP, updated_at TIMESTAMP)
                """);
        jdbcTemplate.execute("""
                CREATE TABLE users (id BIGINT PRIMARY KEY, tenant_id BIGINT, username VARCHAR(64), password_hash VARCHAR(255), display_name VARCHAR(128), email VARCHAR(128), status VARCHAR(32), created_at TIMESTAMP, updated_at TIMESTAMP, created_by BIGINT, updated_by BIGINT)
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
                CREATE TABLE import_job (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    import_type VARCHAR(64) NOT NULL,
                    source_type VARCHAR(32) NOT NULL,
                    source_filename VARCHAR(255) NOT NULL,
                    storage_key VARCHAR(512) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    requested_by BIGINT NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    total_count INT NOT NULL,
                    success_count INT NOT NULL,
                    failure_count INT NOT NULL,
                    error_summary VARCHAR(512),
                    created_at TIMESTAMP NOT NULL,
                    started_at TIMESTAMP,
                    finished_at TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE import_job_item_error (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    import_job_id BIGINT NOT NULL,
                    row_number INT,
                    error_code VARCHAR(64) NOT NULL,
                    error_message VARCHAR(512) NOT NULL,
                    raw_payload CLOB,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.execute("INSERT INTO tenant(id, tenant_code, tenant_name, status, created_at, updated_at) VALUES (1, 'demo-shop', 'Demo', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.execute("INSERT INTO tenant(id, tenant_code, tenant_name, status, created_at, updated_at) VALUES (2, 'other-shop', 'Other', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.execute("INSERT INTO users(id, tenant_id, username, password_hash, display_name, email, status, created_at, updated_at) VALUES (101,1,'admin','x','Admin','a@a','ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        jdbcTemplate.execute("INSERT INTO users(id, tenant_id, username, password_hash, display_name, email, status, created_at, updated_at) VALUES (201,2,'other','x','Other','b@b','ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");

        Path root = Path.of("target/test-imports");
        if (Files.exists(root)) {
            try (var walk = Files.walk(root)) {
                walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                });
            }
        }
    }

    @Test
    void createListDetailAndWorkerShouldRespectTenantAndReachTerminalStatus() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv",
                "username,email\nvalid,user@example.com\ninvalid-row".getBytes());

        ImportJobDetailResponse created = importJobCommandService.createJob(1L, 101L, "req-import-1", request, file);
        assertThat(created.status()).isEqualTo("QUEUED");

        assertThat(importJobQueryService.pageJobs(1L, null).items()).hasSize(1);
        assertThat(importJobQueryService.pageJobs(2L, null).items()).isEmpty();

        importJobWorker.consume(new ImportJobMessage(created.id(), 1L));

        ImportJobDetailResponse processed = importJobQueryService.getJobDetail(1L, created.id());
        assertThat(processed.status()).isEqualTo("SUCCEEDED");
        assertThat(processed.failureCount()).isEqualTo(1);
        assertThat(processed.itemErrors()).hasSize(1);

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE tenant_id = 1 AND entity_type = 'IMPORT_JOB'", Integer.class);
        assertThat(auditCount).isNotNull().isGreaterThanOrEqualTo(2);
    }
}
