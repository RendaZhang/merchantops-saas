package com.renda.merchantops.api.integration;

import com.renda.merchantops.api.MerchantOpsApplication;
import com.renda.merchantops.api.config.ImportProcessingProperties;
import com.renda.merchantops.api.dto.audit.query.AuditEventListResponse;
import com.renda.merchantops.api.dto.audit.query.AuditEventResponse;
import com.renda.merchantops.api.dto.importjob.command.ImportJobCreateRequest;
import com.renda.merchantops.api.dto.importjob.command.ImportJobEditedReplayItemRequest;
import com.renda.merchantops.api.dto.importjob.command.ImportJobEditedReplayRequest;
import com.renda.merchantops.api.dto.importjob.command.ImportJobSelectiveReplayRequest;
import com.renda.merchantops.api.dto.importjob.query.ImportJobDetailResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobErrorCodeCountResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobErrorItemResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobErrorPageQuery;
import com.renda.merchantops.api.dto.importjob.query.ImportJobErrorPageResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobPageQuery;
import com.renda.merchantops.api.dto.importjob.query.ImportJobPageResponse;
import com.renda.merchantops.api.messaging.ImportJobMessage;
import com.renda.merchantops.api.messaging.ImportJobExecutionService;
import com.renda.merchantops.api.messaging.ImportJobPublisher;
import com.renda.merchantops.api.messaging.ImportJobQueueRecoveryService;
import com.renda.merchantops.api.messaging.ImportJobWorker;
import com.renda.merchantops.api.service.AuditEventService;
import com.renda.merchantops.api.service.ImportJobCommandService;
import com.renda.merchantops.api.service.ImportJobQueryService;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
                "spring.task.scheduling.enabled=false",
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "merchantops.import.storage.local-dir=target/test-imports",
                "merchantops.import.processing.chunk-size=2",
                "merchantops.import.processing.max-rows-per-job=1000",
                "merchantops.import.processing.enqueue-recovery-delay-ms=600000"
        }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ImportJobIntegrationTest {

    private static final Path STORAGE_ROOT = Path.of("target/test-imports");

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ImportJobCommandService importJobCommandService;
    @Autowired
    private ImportJobQueryService importJobQueryService;
    @Autowired
    private ImportJobWorker importJobWorker;
    @Autowired
    private AuditEventService auditEventService;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private ImportProcessingProperties importProcessingProperties;
    @Autowired
    private ImportJobQueueRecoveryService importJobQueueRecoveryService;

    @MockBean
    private ImportJobPublisher importJobPublisher;
    @SpyBean
    private ImportJobExecutionService importJobExecutionService;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("DROP ALL OBJECTS");
        jdbcTemplate.execute("""
                CREATE TABLE tenant (id BIGINT PRIMARY KEY, tenant_code VARCHAR(64), tenant_name VARCHAR(128), status VARCHAR(32), created_at TIMESTAMP, updated_at TIMESTAMP)
                """);
        jdbcTemplate.execute("""
                CREATE TABLE users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    tenant_id BIGINT,
                    username VARCHAR(64),
                    password_hash VARCHAR(255),
                    display_name VARCHAR(128),
                    email VARCHAR(128),
                    status VARCHAR(32),
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP,
                    created_by BIGINT,
                    updated_by BIGINT,
                    UNIQUE KEY uk_users_tenant_username (tenant_id, username)
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
                CREATE TABLE user_role (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    role_id BIGINT NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE permission (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    permission_code VARCHAR(64) NOT NULL,
                    permission_name VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE role_permission (
                    role_id BIGINT NOT NULL,
                    permission_id BIGINT NOT NULL,
                    PRIMARY KEY (role_id, permission_id)
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
                CREATE TABLE import_job (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    import_type VARCHAR(64) NOT NULL,
                    source_type VARCHAR(32) NOT NULL,
                    source_filename VARCHAR(255) NOT NULL,
                    storage_key VARCHAR(512) NOT NULL,
                    source_job_id BIGINT,
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
                    source_row_number INT,
                    error_code VARCHAR(64) NOT NULL,
                    error_message VARCHAR(512) NOT NULL,
                    raw_payload CLOB,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.execute("INSERT INTO tenant(id, tenant_code, tenant_name, status, created_at, updated_at) VALUES (1, 'demo-shop', 'Demo', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.execute("INSERT INTO tenant(id, tenant_code, tenant_name, status, created_at, updated_at) VALUES (2, 'other-shop', 'Other', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.execute("INSERT INTO users(id, tenant_id, username, password_hash, display_name, email, status, created_at, updated_at, created_by, updated_by) VALUES (101,1,'admin','x','Admin','a@a.com','ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,101,101)");
        jdbcTemplate.execute("INSERT INTO users(id, tenant_id, username, password_hash, display_name, email, status, created_at, updated_at, created_by, updated_by) VALUES (201,2,'other','x','Other','b@b.com','ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,201,201)");
        jdbcTemplate.execute("INSERT INTO `role`(id, tenant_id, role_code, role_name, created_at, updated_at) VALUES (11,1,'READ_ONLY','Read only',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        jdbcTemplate.execute("INSERT INTO `role`(id, tenant_id, role_code, role_name, created_at, updated_at) VALUES (21,2,'READ_ONLY','Read only',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");

        if (Files.exists(STORAGE_ROOT)) {
            try (var walk = Files.walk(STORAGE_ROOT)) {
                walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                });
            }
        }
        Mockito.reset(importJobPublisher, importJobExecutionService);
        importProcessingProperties.setChunkSize(2);
        importProcessingProperties.setMaxRowsPerJob(1000);
        importProcessingProperties.setStaleProcessingThresholdSeconds(300);
        importProcessingProperties.setEnqueueRecoveryBatchSize(100);
        importProcessingProperties.setEnqueueRecoveryMinAgeSeconds(60);
    }

    @Test
    void workerShouldCreateTenantUsersAndKeepPerRowFailuresIsolated() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv", ("""
                username,displayName,email,password,roleCodes
                alpha,Alpha User,alpha@example.com,abc123,READ_ONLY
                admin,Duplicate User,dup@example.com,abc123,READ_ONLY
                beta,Beta User,beta@example.com,abc123,UNKNOWN_ROLE
                gamma,Gamma User,gamma@example.com,abc123,READ_ONLY
                """).getBytes());

        ImportJobDetailResponse created = importJobCommandService.createJob(1L, 101L, "req-import-1", request, file);
        importJobWorker.consume(new ImportJobMessage(created.id(), 1L));

        ImportJobDetailResponse processed = importJobQueryService.getJobDetail(1L, created.id());
        assertThat(processed.status()).isEqualTo("SUCCEEDED");
        assertThat(processed.totalCount()).isEqualTo(4);
        assertThat(processed.successCount()).isEqualTo(2);
        assertThat(processed.failureCount()).isEqualTo(2);
        assertThat(processed.errorSummary()).isEqualTo("completed with some row errors");
        assertThat(processed.errorCodeCounts()).containsExactlyInAnyOrder(
                new ImportJobErrorCodeCountResponse("DUPLICATE_USERNAME", 1),
                new ImportJobErrorCodeCountResponse("UNKNOWN_ROLE", 1)
        );
        assertThat(processed.itemErrors()).hasSize(2);
        assertThat(processed.itemErrors()).extracting(com.renda.merchantops.api.dto.importjob.query.ImportJobErrorItemResponse::rowNumber)
                .containsExactly(3, 4);
        assertThat(processed.itemErrors().stream().map(item -> item.errorCode()).collect(java.util.stream.Collectors.toSet()))
                .containsExactlyInAnyOrder("DUPLICATE_USERNAME", "UNKNOWN_ROLE");
        ImportJobErrorPageResponse errorPage = importJobQueryService.pageJobErrors(1L, created.id(), new ImportJobErrorPageQuery(0, 10, null));
        assertThat(errorPage.total()).isEqualTo(2);
        assertThat(errorPage.items()).extracting(com.renda.merchantops.api.dto.importjob.query.ImportJobErrorItemResponse::rowNumber)
                .containsExactly(3, 4);
        ImportJobErrorPageResponse unknownRolePage = importJobQueryService.pageJobErrors(1L, created.id(), new ImportJobErrorPageQuery(0, 10, "UNKNOWN_ROLE"));
        assertThat(unknownRolePage.items()).singleElement().satisfies(item -> {
            assertThat(item.rowNumber()).isEqualTo(4);
            assertThat(item.errorCode()).isEqualTo("UNKNOWN_ROLE");
        });

        Integer createdUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE tenant_id = 1 AND username IN ('alpha','gamma')", Integer.class);
        assertThat(createdUsers).isEqualTo(2);
        Integer crossTenantCreated = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE tenant_id = 2 AND username IN ('alpha','gamma')", Integer.class);
        assertThat(crossTenantCreated).isZero();

        Long createdBy = jdbcTemplate.queryForObject(
                "SELECT created_by FROM users WHERE tenant_id = 1 AND username = 'alpha'", Long.class);
        Long updatedBy = jdbcTemplate.queryForObject(
                "SELECT updated_by FROM users WHERE tenant_id = 1 AND username = 'alpha'", Long.class);
        assertThat(createdBy).isEqualTo(101L);
        assertThat(updatedBy).isEqualTo(101L);

        Integer userCreatedAudit = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE tenant_id = 1 AND entity_type = 'USER' AND action_type = 'USER_CREATED'", Integer.class);
        assertThat(userCreatedAudit).isEqualTo(2);
        Integer importAudit = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE tenant_id = 1 AND entity_type = 'IMPORT_JOB'", Integer.class);
        assertThat(importAudit).isNotNull().isGreaterThanOrEqualTo(2);
    }

    @Test
    void workerShouldExposeIncrementingCountersWhileStillProcessing() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users-progress.csv", "text/csv", ("""
                username,displayName,email,password,roleCodes
                alpha,Alpha User,alpha@example.com,abc123,READ_ONLY
                beta,Beta User,beta@example.com,abc123,READ_ONLY
                gamma,Gamma User,gamma@example.com,abc123,READ_ONLY
                delta,Delta User,delta@example.com,abc123,READ_ONLY
                """).getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse created = importJobCommandService.createJob(1L, 101L, "req-import-progress", request, file);
        AtomicInteger chunkCalls = new AtomicInteger();
        AtomicReference<ImportJobDetailResponse> progressSnapshot = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            if (chunkCalls.incrementAndGet() == 2) {
                progressSnapshot.set(importJobQueryService.getJobDetail(1L, created.id()));
            }
            return invocation.callRealMethod();
        }).when(importJobExecutionService).processChunk(Mockito.any(), Mockito.anyList());

        importJobWorker.consume(new ImportJobMessage(created.id(), 1L));

        assertThat(progressSnapshot.get()).isNotNull();
        assertThat(progressSnapshot.get().status()).isEqualTo("PROCESSING");
        assertThat(progressSnapshot.get().totalCount()).isEqualTo(2);
        assertThat(progressSnapshot.get().successCount()).isEqualTo(2);
        assertThat(progressSnapshot.get().failureCount()).isZero();
        assertThat(progressSnapshot.get().finishedAt()).isNull();

        ImportJobDetailResponse processed = importJobQueryService.getJobDetail(1L, created.id());
        assertThat(processed.status()).isEqualTo("SUCCEEDED");
        assertThat(processed.totalCount()).isEqualTo(4);
        assertThat(processed.successCount()).isEqualTo(4);
        assertThat(processed.failureCount()).isZero();
    }

    @Test
    void workerShouldReprocessStaleProcessingJobOnRedelivery() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users-stale-redelivery.csv", "text/csv", ("""
                username,displayName,email,password,roleCodes
                stale,Stale User,stale@example.com,abc123,READ_ONLY
                """).getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse created = importJobCommandService.createJob(1L, 101L, "req-import-stale-redelivery", request, file);
        jdbcTemplate.update(
                "UPDATE import_job SET status = 'PROCESSING', started_at = DATEADD('SECOND', -900, CURRENT_TIMESTAMP), finished_at = NULL WHERE id = ?",
                created.id()
        );

        importJobWorker.consume(new ImportJobMessage(created.id(), 1L));

        ImportJobDetailResponse processed = importJobQueryService.getJobDetail(1L, created.id());
        assertThat(processed.status()).isEqualTo("SUCCEEDED");
        assertThat(processed.totalCount()).isEqualTo(1);
        assertThat(processed.successCount()).isEqualTo(1);
        assertThat(processed.failureCount()).isZero();

        Integer processingStartedAuditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE tenant_id = 1 AND entity_type = 'IMPORT_JOB' AND entity_id = ? AND action_type = 'IMPORT_JOB_PROCESSING_STARTED'",
                Integer.class,
                created.id()
        );
        assertThat(processingStartedAuditCount).isEqualTo(1);
    }

    @Test
    void workerShouldIgnoreDuplicateImportMessagesAfterFirstSuccessfulRun() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users-duplicate-message.csv", "text/csv", ("""
                username,displayName,email,password,roleCodes
                dup1,Dup One,dup1@example.com,abc123,READ_ONLY
                dup2,Dup Two,dup2@example.com,abc123,READ_ONLY
                """).getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse created = importJobCommandService.createJob(1L, 101L, "req-import-duplicate-message", request, file);
        importJobWorker.consume(new ImportJobMessage(created.id(), 1L));

        ImportJobDetailResponse firstProcessed = importJobQueryService.getJobDetail(1L, created.id());
        Integer userCountAfterFirstRun = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE tenant_id = 1 AND username IN ('dup1','dup2')",
                Integer.class
        );
        Integer processingStartedAuditAfterFirstRun = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE tenant_id = 1 AND entity_type = 'IMPORT_JOB' AND entity_id = ? AND action_type = 'IMPORT_JOB_PROCESSING_STARTED'",
                Integer.class,
                created.id()
        );
        Integer completedAuditAfterFirstRun = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE tenant_id = 1 AND entity_type = 'IMPORT_JOB' AND entity_id = ? AND action_type = 'IMPORT_JOB_COMPLETED'",
                Integer.class,
                created.id()
        );

        importJobWorker.consume(new ImportJobMessage(created.id(), 1L));

        ImportJobDetailResponse secondProcessed = importJobQueryService.getJobDetail(1L, created.id());
        Integer userCountAfterSecondRun = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE tenant_id = 1 AND username IN ('dup1','dup2')",
                Integer.class
        );
        Integer processingStartedAuditAfterSecondRun = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE tenant_id = 1 AND entity_type = 'IMPORT_JOB' AND entity_id = ? AND action_type = 'IMPORT_JOB_PROCESSING_STARTED'",
                Integer.class,
                created.id()
        );
        Integer completedAuditAfterSecondRun = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_event WHERE tenant_id = 1 AND entity_type = 'IMPORT_JOB' AND entity_id = ? AND action_type = 'IMPORT_JOB_COMPLETED'",
                Integer.class,
                created.id()
        );

        assertThat(secondProcessed.status()).isEqualTo("SUCCEEDED");
        assertThat(secondProcessed.totalCount()).isEqualTo(firstProcessed.totalCount());
        assertThat(secondProcessed.successCount()).isEqualTo(firstProcessed.successCount());
        assertThat(secondProcessed.failureCount()).isEqualTo(firstProcessed.failureCount());
        assertThat(userCountAfterSecondRun).isEqualTo(userCountAfterFirstRun);
        assertThat(processingStartedAuditAfterSecondRun).isEqualTo(processingStartedAuditAfterFirstRun);
        assertThat(completedAuditAfterSecondRun).isEqualTo(completedAuditAfterFirstRun);
    }

    @Test
    void replayFailedRowsShouldCreateDerivedJobOnlyForFailedRowsAndPreserveAuditLineage() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users-replay.csv", "text/csv", ("""
                username,displayName,email,password,roleCodes
                alpha,Alpha User,alpha@example.com,abc123,READ_ONLY
                retry,"Retry, User",retry@example.com,abc123,RETRY_ROLE
                gamma,Gamma User,gamma@example.com,abc123,READ_ONLY
                """).getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse sourceCreated = importJobCommandService.createJob(1L, 101L, "req-import-replay-source", request, file);
        importJobWorker.consume(new ImportJobMessage(sourceCreated.id(), 1L));

        ImportJobDetailResponse sourceProcessed = importJobQueryService.getJobDetail(1L, sourceCreated.id());
        assertThat(sourceProcessed.status()).isEqualTo("SUCCEEDED");
        assertThat(sourceProcessed.totalCount()).isEqualTo(3);
        assertThat(sourceProcessed.successCount()).isEqualTo(2);
        assertThat(sourceProcessed.failureCount()).isEqualTo(1);
        assertThat(sourceProcessed.itemErrors()).singleElement().satisfies(item -> {
            assertThat(item.rowNumber()).isEqualTo(3);
            assertThat(item.errorCode()).isEqualTo("UNKNOWN_ROLE");
            assertThat(item.rawPayload()).contains("\"Retry, User\"");
        });

        jdbcTemplate.update("""
                INSERT INTO `role`(id, tenant_id, role_code, role_name, created_at, updated_at)
                VALUES (12, 1, 'RETRY_ROLE', 'Retry Role', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        ImportJobDetailResponse replayCreated = importJobCommandService.replayFailedRows(
                1L,
                101L,
                "req-import-replay-derived",
                sourceCreated.id()
        );

        assertThat(replayCreated.status()).isEqualTo("QUEUED");
        assertThat(replayCreated.sourceJobId()).isEqualTo(sourceCreated.id());
        assertThat(replayCreated.sourceFilename()).isEqualTo("replay-failures-job-" + sourceCreated.id() + ".csv");

        importJobWorker.consume(new ImportJobMessage(replayCreated.id(), 1L));

        ImportJobDetailResponse replayProcessed = importJobQueryService.getJobDetail(1L, replayCreated.id());
        assertThat(replayProcessed.status()).isEqualTo("SUCCEEDED");
        assertThat(replayProcessed.sourceJobId()).isEqualTo(sourceCreated.id());
        assertThat(replayProcessed.totalCount()).isEqualTo(1);
        assertThat(replayProcessed.successCount()).isEqualTo(1);
        assertThat(replayProcessed.failureCount()).isZero();
        assertThat(replayProcessed.errorSummary()).isNull();

        Integer alphaCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE tenant_id = 1 AND username = 'alpha'",
                Integer.class
        );
        Integer gammaCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE tenant_id = 1 AND username = 'gamma'",
                Integer.class
        );
        Integer retryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE tenant_id = 1 AND username = 'retry'",
                Integer.class
        );
        String retryDisplayName = jdbcTemplate.queryForObject(
                "SELECT display_name FROM users WHERE tenant_id = 1 AND username = 'retry'",
                String.class
        );
        assertThat(alphaCount).isEqualTo(1);
        assertThat(gammaCount).isEqualTo(1);
        assertThat(retryCount).isEqualTo(1);
        assertThat(retryDisplayName).isEqualTo("Retry, User");

        AuditEventListResponse sourceAudit = auditEventService.listByEntity(1L, "IMPORT_JOB", sourceCreated.id());
        AuditEventListResponse replayAudit = auditEventService.listByEntity(1L, "IMPORT_JOB", replayCreated.id());
        assertThat(sourceAudit.items()).extracting(AuditEventResponse::actionType)
                .contains("IMPORT_JOB_REPLAY_REQUESTED");
        assertThat(replayAudit.items()).extracting(AuditEventResponse::actionType)
                .contains("IMPORT_JOB_CREATED", "IMPORT_JOB_PROCESSING_STARTED", "IMPORT_JOB_COMPLETED");

        AuditEventResponse replayRequested = sourceAudit.items().stream()
                .filter(item -> "IMPORT_JOB_REPLAY_REQUESTED".equals(item.actionType()))
                .findFirst()
                .orElseThrow();
        AuditEventResponse replayCreatedAudit = replayAudit.items().stream()
                .filter(item -> "IMPORT_JOB_CREATED".equals(item.actionType()))
                .findFirst()
                .orElseThrow();
        assertThat(replayRequested.afterValue()).contains("\"replayJobId\":" + replayCreated.id());
        assertThat(replayCreatedAudit.afterValue()).contains("\"sourceJobId\":" + sourceCreated.id());
    }

    @Test
    void replayWholeFileShouldCreateDerivedJobForFailedSourceFileAndPersistWholeFileAuditMode() throws Exception {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users-replay-file.csv", "text/csv", ("""
                username,displayName,email,password,roleCodes
                retry-a,Retry A,retry-a@example.com,abc123,RETRY_ROLE
                retry-b,Retry B,retry-b@example.com,abc123,RETRY_ROLE
                """).getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse sourceCreated = importJobCommandService.createJob(1L, 101L, "req-import-replay-file-source", request, file);
        importJobWorker.consume(new ImportJobMessage(sourceCreated.id(), 1L));

        ImportJobDetailResponse sourceProcessed = importJobQueryService.getJobDetail(1L, sourceCreated.id());
        assertThat(sourceProcessed.status()).isEqualTo("FAILED");
        assertThat(sourceProcessed.totalCount()).isEqualTo(2);
        assertThat(sourceProcessed.successCount()).isZero();
        assertThat(sourceProcessed.failureCount()).isEqualTo(2);
        assertThat(sourceProcessed.errorSummary()).isEqualTo("all rows failed validation");

        jdbcTemplate.update("""
                INSERT INTO `role`(id, tenant_id, role_code, role_name, created_at, updated_at)
                VALUES (12, 1, 'RETRY_ROLE', 'Retry Role', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        ImportJobDetailResponse replayCreated = importJobCommandService.replayWholeFile(
                1L,
                101L,
                "req-import-replay-file-derived",
                sourceCreated.id()
        );

        assertThat(replayCreated.status()).isEqualTo("QUEUED");
        assertThat(replayCreated.sourceJobId()).isEqualTo(sourceCreated.id());
        assertThat(replayCreated.sourceFilename()).isEqualTo("replay-file-job-" + sourceCreated.id() + ".csv");
        assertThat(Files.readString(STORAGE_ROOT.resolve(sourceCreated.storageKey())))
                .isEqualTo(Files.readString(STORAGE_ROOT.resolve(replayCreated.storageKey())));

        importJobWorker.consume(new ImportJobMessage(replayCreated.id(), 1L));

        ImportJobDetailResponse replayProcessed = importJobQueryService.getJobDetail(1L, replayCreated.id());
        assertThat(replayProcessed.status()).isEqualTo("SUCCEEDED");
        assertThat(replayProcessed.sourceJobId()).isEqualTo(sourceCreated.id());
        assertThat(replayProcessed.totalCount()).isEqualTo(2);
        assertThat(replayProcessed.successCount()).isEqualTo(2);
        assertThat(replayProcessed.failureCount()).isZero();

        Integer retryACount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE tenant_id = 1 AND username = 'retry-a'",
                Integer.class
        );
        Integer retryBCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE tenant_id = 1 AND username = 'retry-b'",
                Integer.class
        );
        assertThat(retryACount).isEqualTo(1);
        assertThat(retryBCount).isEqualTo(1);

        AuditEventListResponse sourceAudit = auditEventService.listByEntity(1L, "IMPORT_JOB", sourceCreated.id());
        AuditEventListResponse replayAudit = auditEventService.listByEntity(1L, "IMPORT_JOB", replayCreated.id());
        AuditEventResponse replayRequested = sourceAudit.items().stream()
                .filter(item -> "IMPORT_JOB_REPLAY_REQUESTED".equals(item.actionType()))
                .findFirst()
                .orElseThrow();
        AuditEventResponse replayCreatedAudit = replayAudit.items().stream()
                .filter(item -> "IMPORT_JOB_CREATED".equals(item.actionType()))
                .findFirst()
                .orElseThrow();
        assertThat(replayRequested.afterValue()).contains("\"replayJobId\":" + replayCreated.id());
        assertThat(replayRequested.afterValue()).contains("\"replayMode\":\"WHOLE_FILE\"");
        assertThat(replayCreatedAudit.afterValue()).contains("\"sourceJobId\":" + sourceCreated.id());
        assertThat(replayCreatedAudit.afterValue()).contains("\"replayMode\":\"WHOLE_FILE\"");
    }

    @Test
    void replayWholeFileShouldRejectSucceededSourceJobs() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users-replay-file-succeeded.csv", "text/csv", ("""
                username,displayName,email,password,roleCodes
                alpha,Alpha User,alpha@example.com,abc123,READ_ONLY
                retry,Retry User,retry@example.com,abc123,RETRY_ROLE
                """).getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse sourceCreated = importJobCommandService.createJob(1L, 101L, "req-import-replay-file-succeeded-source", request, file);
        importJobWorker.consume(new ImportJobMessage(sourceCreated.id(), 1L));

        ImportJobDetailResponse sourceProcessed = importJobQueryService.getJobDetail(1L, sourceCreated.id());
        assertThat(sourceProcessed.status()).isEqualTo("SUCCEEDED");
        assertThat(sourceProcessed.successCount()).isEqualTo(1);
        assertThat(sourceProcessed.failureCount()).isEqualTo(1);

        assertThatThrownBy(() -> importJobCommandService.replayWholeFile(
                1L,
                101L,
                "req-import-replay-file-succeeded-derived",
                sourceCreated.id()
        ))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(biz.getMessage()).contains("FAILED source jobs");
                });
    }

    @Test
    void replayWholeFileShouldRejectFailedSourceJobsThatAlreadySucceededSomeRows() {
        importProcessingProperties.setMaxRowsPerJob(1);

        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users-replay-file-partial-failed.csv", "text/csv", ("""
                username,displayName,email,password,roleCodes
                alpha,Alpha User,alpha@example.com,abc123,READ_ONLY
                beta,Beta User,beta@example.com,abc123,READ_ONLY
                """).getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse sourceCreated = importJobCommandService.createJob(1L, 101L, "req-import-replay-file-partial-failed-source", request, file);
        importJobWorker.consume(new ImportJobMessage(sourceCreated.id(), 1L));

        ImportJobDetailResponse sourceProcessed = importJobQueryService.getJobDetail(1L, sourceCreated.id());
        assertThat(sourceProcessed.status()).isEqualTo("FAILED");
        assertThat(sourceProcessed.successCount()).isEqualTo(1);
        assertThat(sourceProcessed.failureCount()).isEqualTo(1);
        assertThat(sourceProcessed.errorCodeCounts()).containsExactly(new ImportJobErrorCodeCountResponse("MAX_ROWS_EXCEEDED", 1));

        assertThatThrownBy(() -> importJobCommandService.replayWholeFile(
                1L,
                101L,
                "req-import-replay-file-partial-failed-derived",
                sourceCreated.id()
        ))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(biz.getMessage()).contains("no successful rows");
                });
    }

    @Test
    void replayWholeFileShouldRejectFailedSourceJobsWithoutRowLevelFailureEvidence() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile invalidHeaderFile = new MockMultipartFile("file", "users-replay-file-invalid-header.csv", "text/csv", ("""
                badHeader,displayName,email,password,roleCodes
                retry,Retry User,retry@example.com,abc123,READ_ONLY
                """).getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse invalidHeaderSource = importJobCommandService.createJob(
                1L,
                101L,
                "req-import-replay-file-invalid-header-source",
                request,
                invalidHeaderFile
        );
        importJobWorker.consume(new ImportJobMessage(invalidHeaderSource.id(), 1L));

        assertThatThrownBy(() -> importJobCommandService.replayWholeFile(
                1L,
                101L,
                "req-import-replay-file-invalid-header-derived",
                invalidHeaderSource.id()
        ))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(biz.getMessage()).contains("row-level failed rows");
                });

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "users-replay-file-empty.csv",
                "text/csv",
                "\n".getBytes(StandardCharsets.UTF_8)
        );
        ImportJobDetailResponse emptySource = importJobCommandService.createJob(
                1L,
                101L,
                "req-import-replay-file-empty-source",
                request,
                emptyFile
        );
        importJobWorker.consume(new ImportJobMessage(emptySource.id(), 1L));

        assertThatThrownBy(() -> importJobCommandService.replayWholeFile(
                1L,
                101L,
                "req-import-replay-file-empty-derived",
                emptySource.id()
        ))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(biz.getMessage()).contains("row-level failed rows");
                });
    }

    @Test
    void replayFailedRowsSelectiveShouldCreateDerivedJobOnlyForRequestedErrorCodesAndPersistAuditSelection() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users-replay-selective.csv", "text/csv", ("""
                username,displayName,email,password,roleCodes
                alpha,Alpha User,alpha@example.com,abc123,READ_ONLY
                retry,Retry User,retry@example.com,abc123,RETRY_ROLE
                bad,Bad User,bad-email,abc123,READ_ONLY
                gamma,Gamma User,gamma@example.com,abc123,READ_ONLY
                """).getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse sourceCreated = importJobCommandService.createJob(1L, 101L, "req-import-replay-selective-source", request, file);
        importJobWorker.consume(new ImportJobMessage(sourceCreated.id(), 1L));

        ImportJobDetailResponse sourceProcessed = importJobQueryService.getJobDetail(1L, sourceCreated.id());
        assertThat(sourceProcessed.status()).isEqualTo("SUCCEEDED");
        assertThat(sourceProcessed.totalCount()).isEqualTo(4);
        assertThat(sourceProcessed.successCount()).isEqualTo(2);
        assertThat(sourceProcessed.failureCount()).isEqualTo(2);
        assertThat(toErrorCountMap(sourceProcessed.errorCodeCounts())).isEqualTo(Map.of(
                "UNKNOWN_ROLE", 1L,
                "INVALID_EMAIL", 1L
        ));

        jdbcTemplate.update("""
                INSERT INTO `role`(id, tenant_id, role_code, role_name, created_at, updated_at)
                VALUES (12, 1, 'RETRY_ROLE', 'Retry Role', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        ImportJobDetailResponse replayCreated = importJobCommandService.replayFailedRowsSelective(
                1L,
                101L,
                "req-import-replay-selective-derived",
                sourceCreated.id(),
                new ImportJobSelectiveReplayRequest(java.util.List.of("UNKNOWN_ROLE"))
        );

        assertThat(replayCreated.status()).isEqualTo("QUEUED");
        assertThat(replayCreated.sourceJobId()).isEqualTo(sourceCreated.id());

        importJobWorker.consume(new ImportJobMessage(replayCreated.id(), 1L));

        ImportJobDetailResponse replayProcessed = importJobQueryService.getJobDetail(1L, replayCreated.id());
        assertThat(replayProcessed.status()).isEqualTo("SUCCEEDED");
        assertThat(replayProcessed.sourceJobId()).isEqualTo(sourceCreated.id());
        assertThat(replayProcessed.totalCount()).isEqualTo(1);
        assertThat(replayProcessed.successCount()).isEqualTo(1);
        assertThat(replayProcessed.failureCount()).isZero();

        Integer retryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE tenant_id = 1 AND username = 'retry'",
                Integer.class
        );
        Integer badCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE tenant_id = 1 AND username = 'bad'",
                Integer.class
        );
        assertThat(retryCount).isEqualTo(1);
        assertThat(badCount).isZero();

        AuditEventListResponse sourceAudit = auditEventService.listByEntity(1L, "IMPORT_JOB", sourceCreated.id());
        AuditEventListResponse replayAudit = auditEventService.listByEntity(1L, "IMPORT_JOB", replayCreated.id());
        AuditEventResponse replayRequested = sourceAudit.items().stream()
                .filter(item -> "IMPORT_JOB_REPLAY_REQUESTED".equals(item.actionType()))
                .findFirst()
                .orElseThrow();
        AuditEventResponse replayCreatedAudit = replayAudit.items().stream()
                .filter(item -> "IMPORT_JOB_CREATED".equals(item.actionType()))
                .findFirst()
                .orElseThrow();
        assertThat(replayRequested.afterValue()).contains("\"replayJobId\":" + replayCreated.id());
        assertThat(replayRequested.afterValue()).contains("\"selectedErrorCodes\":[\"UNKNOWN_ROLE\"]");
        assertThat(replayCreatedAudit.afterValue()).contains("\"sourceJobId\":" + sourceCreated.id());
        assertThat(replayCreatedAudit.afterValue()).contains("\"selectedErrorCodes\":[\"UNKNOWN_ROLE\"]");
    }

    @Test
    void replayFailedRowsSelectiveShouldRejectUnknownRequestedErrorCodes() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users-replay-selective-none.csv", "text/csv", ("""
                username,displayName,email,password,roleCodes
                retry,Retry User,retry@example.com,abc123,RETRY_ROLE
                """).getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse sourceCreated = importJobCommandService.createJob(1L, 101L, "req-import-replay-selective-none-source", request, file);
        importJobWorker.consume(new ImportJobMessage(sourceCreated.id(), 1L));

        assertThatThrownBy(() -> importJobCommandService.replayFailedRowsSelective(
                1L,
                101L,
                "req-import-replay-selective-none-derived",
                sourceCreated.id(),
                new ImportJobSelectiveReplayRequest(java.util.List.of("INVALID_EMAIL"))
        ))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(biz.getMessage()).contains("selected errorCodes");
                });
    }

    @Test
    void replayFailedRowsSelectiveShouldRejectCrossTenantSourceJobs() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users-replay-selective-cross-tenant.csv", "text/csv", ("""
                username,displayName,email,password,roleCodes
                retry,Retry User,retry@example.com,abc123,RETRY_ROLE
                """).getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse sourceCreated = importJobCommandService.createJob(1L, 101L, "req-import-replay-selective-cross-tenant-source", request, file);
        importJobWorker.consume(new ImportJobMessage(sourceCreated.id(), 1L));

        assertThatThrownBy(() -> importJobCommandService.replayFailedRowsSelective(
                2L,
                201L,
                "req-import-replay-selective-cross-tenant-derived",
                sourceCreated.id(),
                new ImportJobSelectiveReplayRequest(java.util.List.of("UNKNOWN_ROLE"))
        ))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void replayFailedRowsEditedShouldCreateDerivedJobForEditedRowsAndKeepAuditScopeOnly() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users-replay-edited.csv", "text/csv", ("""
                username,displayName,email,password,roleCodes
                alpha,Alpha User,alpha@example.com,abc123,READ_ONLY
                retry-role,Retry Role User,retry-role@example.com,abc123,RETRY_ROLE
                retry-email,Retry Email User,bad-email,abc123,READ_ONLY
                gamma,Gamma User,gamma@example.com,abc123,READ_ONLY
                """).getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse sourceCreated = importJobCommandService.createJob(1L, 101L, "req-import-replay-edited-source", request, file);
        importJobWorker.consume(new ImportJobMessage(sourceCreated.id(), 1L));

        ImportJobDetailResponse sourceProcessed = importJobQueryService.getJobDetail(1L, sourceCreated.id());
        assertThat(sourceProcessed.status()).isEqualTo("SUCCEEDED");
        assertThat(sourceProcessed.failureCount()).isEqualTo(2);
        Map<String, ImportJobErrorItemResponse> errorsByCode = sourceProcessed.itemErrors().stream()
                .collect(Collectors.toMap(ImportJobErrorItemResponse::errorCode, Function.identity()));
        Long unknownRoleErrorId = errorsByCode.get("UNKNOWN_ROLE").id();
        Long invalidEmailErrorId = errorsByCode.get("INVALID_EMAIL").id();

        jdbcTemplate.update("""
                INSERT INTO `role`(id, tenant_id, role_code, role_name, created_at, updated_at)
                VALUES (12, 1, 'RETRY_ROLE', 'Retry Role', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        ImportJobEditedReplayRequest editedRequest = new ImportJobEditedReplayRequest(java.util.List.of(
                new ImportJobEditedReplayItemRequest(
                        unknownRoleErrorId,
                        "retry-role",
                        "Retry Role User Fixed",
                        "retry-role@example.com",
                        "new-pass-role",
                        java.util.List.of("RETRY_ROLE")
                ),
                new ImportJobEditedReplayItemRequest(
                        invalidEmailErrorId,
                        "retry-email",
                        "Retry Email User Fixed",
                        "retry-email@example.com",
                        "new-pass-email",
                        java.util.List.of("READ_ONLY")
                )
        ));

        ImportJobDetailResponse replayCreated = importJobCommandService.replayFailedRowsEdited(
                1L,
                101L,
                "req-import-replay-edited-derived",
                sourceCreated.id(),
                editedRequest
        );

        assertThat(replayCreated.status()).isEqualTo("QUEUED");
        assertThat(replayCreated.sourceJobId()).isEqualTo(sourceCreated.id());
        assertThat(replayCreated.sourceFilename()).isEqualTo("replay-edited-job-" + sourceCreated.id() + ".csv");

        importJobWorker.consume(new ImportJobMessage(replayCreated.id(), 1L));

        ImportJobDetailResponse replayProcessed = importJobQueryService.getJobDetail(1L, replayCreated.id());
        assertThat(replayProcessed.status()).isEqualTo("SUCCEEDED");
        assertThat(replayProcessed.sourceJobId()).isEqualTo(sourceCreated.id());
        assertThat(replayProcessed.totalCount()).isEqualTo(2);
        assertThat(replayProcessed.successCount()).isEqualTo(2);
        assertThat(replayProcessed.failureCount()).isZero();

        String retryRoleDisplayName = jdbcTemplate.queryForObject(
                "SELECT display_name FROM users WHERE tenant_id = 1 AND username = 'retry-role'",
                String.class
        );
        String retryEmail = jdbcTemplate.queryForObject(
                "SELECT email FROM users WHERE tenant_id = 1 AND username = 'retry-email'",
                String.class
        );
        assertThat(retryRoleDisplayName).isEqualTo("Retry Role User Fixed");
        assertThat(retryEmail).isEqualTo("retry-email@example.com");

        AuditEventListResponse sourceAudit = auditEventService.listByEntity(1L, "IMPORT_JOB", sourceCreated.id());
        AuditEventListResponse replayAudit = auditEventService.listByEntity(1L, "IMPORT_JOB", replayCreated.id());
        AuditEventResponse replayRequested = sourceAudit.items().stream()
                .filter(item -> "IMPORT_JOB_REPLAY_REQUESTED".equals(item.actionType()))
                .findFirst()
                .orElseThrow();
        AuditEventResponse replayCreatedAudit = replayAudit.items().stream()
                .filter(item -> "IMPORT_JOB_CREATED".equals(item.actionType()))
                .findFirst()
                .orElseThrow();
        assertThat(replayRequested.afterValue()).contains("\"replayJobId\":" + replayCreated.id());
        assertThat(replayRequested.afterValue()).contains("\"editedErrorIds\":[" + unknownRoleErrorId + "," + invalidEmailErrorId + "]");
        assertThat(replayRequested.afterValue()).contains("\"editedRowCount\":2");
        assertThat(replayRequested.afterValue()).contains("\"editedFields\":[\"username\",\"displayName\",\"email\",\"password\",\"roleCodes\"]");
        assertThat(replayRequested.afterValue()).doesNotContain("new-pass-role").doesNotContain("new-pass-email");
        assertThat(replayRequested.afterValue()).doesNotContain("retry-email@example.com").doesNotContain("Retry Role User Fixed");
        assertThat(replayCreatedAudit.afterValue()).contains("\"sourceJobId\":" + sourceCreated.id());
        assertThat(replayCreatedAudit.afterValue()).contains("\"editedErrorIds\":[" + unknownRoleErrorId + "," + invalidEmailErrorId + "]");
        assertThat(replayCreatedAudit.afterValue()).contains("\"editedRowCount\":2");
        assertThat(replayCreatedAudit.afterValue()).doesNotContain("new-pass-role").doesNotContain("new-pass-email");
        assertThat(replayCreatedAudit.afterValue()).doesNotContain("retry-email@example.com").doesNotContain("Retry Role User Fixed");
    }

    @Test
    void replayFailedRowsEditedShouldProcessOnlyRequestedErrorIds() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users-replay-edited-subset.csv", "text/csv", ("""
                username,displayName,email,password,roleCodes
                alpha,Alpha User,alpha@example.com,abc123,READ_ONLY
                retry-role,Retry Role User,retry-role@example.com,abc123,RETRY_ROLE
                retry-email,Retry Email User,bad-email,abc123,READ_ONLY
                """).getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse sourceCreated = importJobCommandService.createJob(1L, 101L, "req-import-replay-edited-subset-source", request, file);
        importJobWorker.consume(new ImportJobMessage(sourceCreated.id(), 1L));

        ImportJobDetailResponse sourceProcessed = importJobQueryService.getJobDetail(1L, sourceCreated.id());
        Long invalidEmailErrorId = sourceProcessed.itemErrors().stream()
                .filter(item -> "INVALID_EMAIL".equals(item.errorCode()))
                .findFirst()
                .orElseThrow()
                .id();

        ImportJobEditedReplayRequest editedRequest = new ImportJobEditedReplayRequest(java.util.List.of(
                new ImportJobEditedReplayItemRequest(
                        invalidEmailErrorId,
                        "retry-email",
                        "Retry Email User Fixed",
                        "retry-email@example.com",
                        "new-pass-email",
                        java.util.List.of("READ_ONLY")
                )
        ));

        ImportJobDetailResponse replayCreated = importJobCommandService.replayFailedRowsEdited(
                1L,
                101L,
                "req-import-replay-edited-subset-derived",
                sourceCreated.id(),
                editedRequest
        );
        importJobWorker.consume(new ImportJobMessage(replayCreated.id(), 1L));

        ImportJobDetailResponse replayProcessed = importJobQueryService.getJobDetail(1L, replayCreated.id());
        assertThat(replayProcessed.status()).isEqualTo("SUCCEEDED");
        assertThat(replayProcessed.totalCount()).isEqualTo(1);
        assertThat(replayProcessed.successCount()).isEqualTo(1);
        assertThat(replayProcessed.failureCount()).isZero();

        Integer retryRoleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE tenant_id = 1 AND username = 'retry-role'",
                Integer.class
        );
        Integer retryEmailCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE tenant_id = 1 AND username = 'retry-email'",
                Integer.class
        );
        assertThat(retryRoleCount).isZero();
        assertThat(retryEmailCount).isEqualTo(1);
    }

    @Test
    void replayFailedRowsEditedShouldRejectHeaderOrGlobalErrors() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users-replay-edited-invalid-header.csv", "text/csv", ("""
                username,email,password,roleCodes
                broken,Broken User,broken@example.com,abc123,READ_ONLY
                """).getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse sourceCreated = importJobCommandService.createJob(1L, 101L, "req-import-replay-edited-invalid-header-source", request, file);
        importJobWorker.consume(new ImportJobMessage(sourceCreated.id(), 1L));

        ImportJobDetailResponse sourceProcessed = importJobQueryService.getJobDetail(1L, sourceCreated.id());
        Long invalidHeaderErrorId = sourceProcessed.itemErrors().get(0).id();

        ImportJobEditedReplayRequest editedRequest = new ImportJobEditedReplayRequest(java.util.List.of(
                new ImportJobEditedReplayItemRequest(
                        invalidHeaderErrorId,
                        "broken",
                        "Broken User",
                        "broken@example.com",
                        "123456",
                        java.util.List.of("READ_ONLY")
                )
        ));

        assertThatThrownBy(() -> importJobCommandService.replayFailedRowsEdited(
                1L,
                101L,
                "req-import-replay-edited-invalid-header-derived",
                sourceCreated.id(),
                editedRequest
        ))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(biz.getMessage()).contains("row-level errors with raw payload");
                });
    }

    @Test
    void replayFailedRowsShouldRejectSourceJobsWithoutFailedRows() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users-clean.csv", "text/csv", ("""
                username,displayName,email,password,roleCodes
                clean,Clean User,clean@example.com,abc123,READ_ONLY
                """).getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse sourceCreated = importJobCommandService.createJob(1L, 101L, "req-import-clean", request, file);
        importJobWorker.consume(new ImportJobMessage(sourceCreated.id(), 1L));

        assertThatThrownBy(() -> importJobCommandService.replayFailedRows(1L, 101L, "req-replay-none", sourceCreated.id()))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    void replayFailedRowsShouldRejectSourceJobsBeforeTerminalStatus() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users-pending.csv", "text/csv", ("""
                username,displayName,email,password,roleCodes
                pending,Pending User,pending@example.com,abc123,READ_ONLY
                """).getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse sourceCreated = importJobCommandService.createJob(1L, 101L, "req-import-pending", request, file);

        assertThatThrownBy(() -> importJobCommandService.replayFailedRows(1L, 101L, "req-replay-pending", sourceCreated.id()))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(biz.getMessage()).contains("terminal status");
                });
    }

    @Test
    void workerShouldMarkFailedWhenAllRowsFailBusinessValidation() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users-invalid.csv", "text/csv", ("""
                username,displayName,email,password,roleCodes
                admin,Duplicate User,dup@example.com,abc123,READ_ONLY
                bad,Bad User,bad-email,abc123,READ_ONLY
                edge,Edge User,edge@example.com, 123456,READ_ONLY
                """).getBytes());

        ImportJobDetailResponse created = importJobCommandService.createJob(1L, 101L, "req-import-2", request, file);
        importJobWorker.consume(new ImportJobMessage(created.id(), 1L));

        ImportJobDetailResponse processed = importJobQueryService.getJobDetail(1L, created.id());
        assertThat(processed.status()).isEqualTo("FAILED");
        assertThat(processed.successCount()).isZero();
        assertThat(processed.failureCount()).isEqualTo(3);
        assertThat(processed.errorSummary()).isEqualTo("all rows failed validation");
        assertThat(processed.errorCodeCounts()).containsExactlyInAnyOrder(
                new ImportJobErrorCodeCountResponse("DUPLICATE_USERNAME", 1),
                new ImportJobErrorCodeCountResponse("INVALID_EMAIL", 1),
                new ImportJobErrorCodeCountResponse("INVALID_PASSWORD", 1)
        );
        assertThat(processed.itemErrors()).extracting(com.renda.merchantops.api.dto.importjob.query.ImportJobErrorItemResponse::errorCode)
                .containsExactlyInAnyOrder("DUPLICATE_USERNAME", "INVALID_EMAIL", "INVALID_PASSWORD");
    }

    @Test
    void workerShouldFailWhenMaxRowsPerJobIsExceededAndKeepErrorQueryable() {
        importProcessingProperties.setMaxRowsPerJob(5);

        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users-limit.csv", "text/csv", ("""
                username,displayName,email,password,roleCodes
                u1,User One,u1@example.com,abc123,READ_ONLY
                u2,User Two,u2@example.com,abc123,READ_ONLY
                u3,User Three,u3@example.com,abc123,READ_ONLY
                u4,User Four,u4@example.com,abc123,READ_ONLY
                u5,User Five,u5@example.com,abc123,READ_ONLY
                u6,User Six,u6@example.com,abc123,READ_ONLY
                """).getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse created = importJobCommandService.createJob(1L, 101L, "req-import-limit", request, file);
        importJobWorker.consume(new ImportJobMessage(created.id(), 1L));

        ImportJobDetailResponse processed = importJobQueryService.getJobDetail(1L, created.id());
        assertThat(processed.status()).isEqualTo("FAILED");
        assertThat(processed.totalCount()).isEqualTo(6);
        assertThat(processed.successCount()).isEqualTo(5);
        assertThat(processed.failureCount()).isEqualTo(1);
        assertThat(processed.errorSummary()).isEqualTo("import job exceeded max row limit");
        assertThat(processed.errorCodeCounts()).containsExactly(new ImportJobErrorCodeCountResponse("MAX_ROWS_EXCEEDED", 1));
        assertThat(processed.itemErrors()).singleElement().satisfies(item -> {
            assertThat(item.rowNumber()).isNull();
            assertThat(item.errorCode()).isEqualTo("MAX_ROWS_EXCEEDED");
            assertThat(item.rawPayload()).isEqualTo("u6,User Six,u6@example.com,abc123,READ_ONLY");
        });

        Integer createdUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE tenant_id = 1 AND username IN ('u1','u2','u3','u4','u5','u6')",
                Integer.class
        );
        assertThat(createdUsers).isEqualTo(5);

        ImportJobErrorPageResponse errorPage = importJobQueryService.pageJobErrors(1L, created.id(), new ImportJobErrorPageQuery(0, 10, "MAX_ROWS_EXCEEDED"));
        assertThat(errorPage.items()).singleElement().satisfies(item -> {
            assertThat(item.errorCode()).isEqualTo("MAX_ROWS_EXCEEDED");
            assertThat(item.rawPayload()).isEqualTo("u6,User Six,u6@example.com,abc123,READ_ONLY");
        });
    }

    @Test
    void workerShouldPersistQuotedCsvFieldsThatContainCommaAndEscapedQuotes() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users-quoted.csv", "text/csv", ("""
                username,displayName,email,password,roleCodes
                quoted,"Escaped ""Quote"", User",quoted@example.com,abc123,READ_ONLY
                """).getBytes());

        ImportJobDetailResponse created = importJobCommandService.createJob(1L, 101L, "req-import-quoted", request, file);
        importJobWorker.consume(new ImportJobMessage(created.id(), 1L));

        ImportJobDetailResponse processed = importJobQueryService.getJobDetail(1L, created.id());
        assertThat(processed.status()).isEqualTo("SUCCEEDED");
        assertThat(processed.totalCount()).isEqualTo(1);
        assertThat(processed.successCount()).isEqualTo(1);
        assertThat(processed.failureCount()).isZero();
        assertThat(processed.errorSummary()).isNull();

        String displayName = jdbcTemplate.queryForObject(
                "SELECT display_name FROM users WHERE tenant_id = 1 AND username = 'quoted'", String.class);
        assertThat(displayName).isEqualTo("Escaped \"Quote\", User");
    }

    @Test
    void workerShouldAcceptBomHeaderAndPersistRowRequestIdWithinSchemaLimit() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        String longRequestId = "req-" + "x".repeat(180);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users-bom.csv",
                "text/csv",
                ("\uFEFFusername,displayName,email,password,roleCodes\n" +
                        "bomuser,Bom User,bom@example.com,abc123,READ_ONLY\n").getBytes(StandardCharsets.UTF_8)
        );

        ImportJobDetailResponse created = importJobCommandService.createJob(1L, 101L, longRequestId, request, file);
        assertThat(created.requestId()).hasSizeLessThanOrEqualTo(128);

        importJobWorker.consume(new ImportJobMessage(created.id(), 1L));

        ImportJobDetailResponse processed = importJobQueryService.getJobDetail(1L, created.id());
        assertThat(processed.status()).isEqualTo("SUCCEEDED");
        assertThat(processed.totalCount()).isEqualTo(1);
        assertThat(processed.successCount()).isEqualTo(1);
        assertThat(processed.failureCount()).isZero();
        assertThat(processed.errorSummary()).isNull();

        String persistedJobRequestId = jdbcTemplate.queryForObject(
                "SELECT request_id FROM import_job WHERE id = ?",
                String.class,
                created.id()
        );
        assertThat(persistedJobRequestId).hasSizeLessThanOrEqualTo(128);

        String rowAuditRequestId = jdbcTemplate.queryForObject("""
                SELECT ae.request_id
                FROM audit_event ae
                JOIN users u ON u.id = ae.entity_id
                WHERE ae.tenant_id = 1
                  AND ae.entity_type = 'USER'
                  AND ae.action_type = 'USER_CREATED'
                  AND u.username = 'bomuser'
                """, String.class);
        assertThat(rowAuditRequestId).hasSizeLessThanOrEqualTo(128);
        assertThat(rowAuditRequestId).endsWith("-r2");
    }

    @Test
    void createJobShouldPublishMessageOnlyAfterCommit() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv",
                "username,displayName,email,password,roleCodes\nvalid,Valid User,valid@example.com,123456,READ_ONLY".getBytes());
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        AtomicLong createdId = new AtomicLong();

        transactionTemplate.executeWithoutResult(status -> {
            ImportJobDetailResponse created = importJobCommandService.createJob(1L, 101L, "req-import-after-commit", request, file);
            createdId.set(created.id());
            verifyNoInteractions(importJobPublisher);
        });

        verify(importJobPublisher).publish(new ImportJobMessage(createdId.get(), 1L));
    }

    @Test
    void createJobShouldNotPublishMessageWhenTransactionRollsBack() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv",
                "username,displayName,email,password,roleCodes\nvalid,Valid User,valid@example.com,123456,READ_ONLY".getBytes());
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        AtomicLong fileCountInsideTransaction = new AtomicLong();

        transactionTemplate.executeWithoutResult(status -> {
            importJobCommandService.createJob(1L, 101L, "req-import-rollback", request, file);
            fileCountInsideTransaction.set(countStoredFiles());
            status.setRollbackOnly();
            verifyNoInteractions(importJobPublisher);
        });

        verifyNoInteractions(importJobPublisher);
        assertThat(fileCountInsideTransaction.get()).isEqualTo(1L);
        assertThat(countStoredFiles()).isZero();
        Integer jobCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM import_job", Integer.class);
        assertThat(jobCount).isZero();
    }

    @Test
    void queuedJobShouldBeRepublishedByRecoveryWhenAfterCommitPublishFails() {
        importProcessingProperties.setEnqueueRecoveryMinAgeSeconds(0);
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users-queued-recovery.csv",
                "text/csv",
                "username,displayName,email,password,roleCodes\nqueued,Queued User,queued@example.com,123456,READ_ONLY".getBytes(StandardCharsets.UTF_8)
        );
        Mockito.doThrow(new RuntimeException("broker down"))
                .when(importJobPublisher)
                .publish(Mockito.any(ImportJobMessage.class));

        ImportJobDetailResponse created = importJobCommandService.createJob(1L, 101L, "req-import-queued-recovery", request, file);
        Integer queuedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM import_job WHERE id = ? AND tenant_id = 1 AND status = 'QUEUED'",
                Integer.class,
                created.id()
        );
        assertThat(queuedCount).isEqualTo(1);

        Mockito.reset(importJobPublisher);
        int republished = importJobQueueRecoveryService.republishStaleQueuedJobs();

        assertThat(republished).isGreaterThanOrEqualTo(1);
        verify(importJobPublisher).publish(new ImportJobMessage(created.id(), 1L));
    }

    @Test
    void replayRollbackShouldCleanupStagedFilesForAllReplayPaths() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile sourceFile = new MockMultipartFile("file", "users-replay-rollback-source.csv", "text/csv", ("""
                username,displayName,email,password,roleCodes
                retry-role,Retry Role,retry-role@example.com,abc123,RETRY_ROLE
                retry-email,Retry Email,bad-email,abc123,READ_ONLY
                """).getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse sourceCreated = importJobCommandService.createJob(
                1L,
                101L,
                "req-import-replay-rollback-source",
                request,
                sourceFile
        );
        importJobWorker.consume(new ImportJobMessage(sourceCreated.id(), 1L));

        ImportJobDetailResponse sourceProcessed = importJobQueryService.getJobDetail(1L, sourceCreated.id());
        assertThat(sourceProcessed.status()).isEqualTo("FAILED");
        assertThat(sourceProcessed.successCount()).isZero();
        assertThat(sourceProcessed.failureCount()).isEqualTo(2);

        Long unknownRoleErrorId = sourceProcessed.itemErrors().stream()
                .filter(item -> "UNKNOWN_ROLE".equals(item.errorCode()))
                .map(ImportJobErrorItemResponse::id)
                .findFirst()
                .orElseThrow();
        Long invalidEmailErrorId = sourceProcessed.itemErrors().stream()
                .filter(item -> "INVALID_EMAIL".equals(item.errorCode()))
                .map(ImportJobErrorItemResponse::id)
                .findFirst()
                .orElseThrow();

        assertReplayRollbackCleansStagedFile(
                "req-replay-rollback-failed-rows",
                () -> importJobCommandService.replayFailedRows(1L, 101L, "req-replay-rollback-failed-rows", sourceCreated.id())
        );
        assertReplayRollbackCleansStagedFile(
                "req-replay-rollback-file",
                () -> importJobCommandService.replayWholeFile(1L, 101L, "req-replay-rollback-file", sourceCreated.id())
        );
        assertReplayRollbackCleansStagedFile(
                "req-replay-rollback-selective",
                () -> importJobCommandService.replayFailedRowsSelective(
                        1L,
                        101L,
                        "req-replay-rollback-selective",
                        sourceCreated.id(),
                        new ImportJobSelectiveReplayRequest(java.util.List.of("UNKNOWN_ROLE"))
                )
        );
        assertReplayRollbackCleansStagedFile(
                "req-replay-rollback-edited",
                () -> importJobCommandService.replayFailedRowsEdited(
                        1L,
                        101L,
                        "req-replay-rollback-edited",
                        sourceCreated.id(),
                        new ImportJobEditedReplayRequest(java.util.List.of(
                                new ImportJobEditedReplayItemRequest(
                                        unknownRoleErrorId,
                                        "retry-role-fixed",
                                        "Retry Role Fixed",
                                        "retry-role-fixed@example.com",
                                        "abc123",
                                        java.util.List.of("READ_ONLY")
                                ),
                                new ImportJobEditedReplayItemRequest(
                                        invalidEmailErrorId,
                                        "retry-email-fixed",
                                        "Retry Email Fixed",
                                        "retry-email-fixed@example.com",
                                        "abc123",
                                        java.util.List.of("READ_ONLY")
                                )
                        ))
                )
        );
    }

    @Test
    void pageJobsShouldApplyTenantScopedFiltersAndStableOrdering() {
        insertImportJob(5101L, 1L, "USER_CSV", "SUCCEEDED", 101L, 2, 2, 0, null, "2026-03-12 10:00:00");
        insertImportJob(5102L, 1L, "USER_CSV", "FAILED", 101L, 2, 0, 2, "all rows failed validation", "2026-03-12 10:05:00");
        insertImportJob(5103L, 1L, "USER_CSV", "SUCCEEDED", 102L, 3, 2, 1, "completed with some row errors", "2026-03-12 10:05:00");
        insertImportJob(5104L, 1L, "ORDER_CSV", "FAILED", 101L, 1, 0, 1, "all rows failed validation", "2026-03-12 09:00:00");
        insertImportJob(5201L, 2L, "USER_CSV", "FAILED", 101L, 4, 0, 4, "all rows failed validation", "2026-03-12 11:00:00");

        ImportJobPageResponse allJobs = importJobQueryService.pageJobs(1L, new ImportJobPageQuery(0, 10, null, null, null, false));
        assertThat(allJobs.items()).extracting(com.renda.merchantops.api.dto.importjob.query.ImportJobListItemResponse::id)
                .containsExactly(5103L, 5102L, 5101L, 5104L);
        assertThat(allJobs.items()).extracting(com.renda.merchantops.api.dto.importjob.query.ImportJobListItemResponse::requestedBy)
                .containsExactly(102L, 101L, 101L, 101L);
        assertThat(allJobs.items()).extracting(com.renda.merchantops.api.dto.importjob.query.ImportJobListItemResponse::hasFailures)
                .containsExactly(true, true, false, true);

        ImportJobPageResponse failedJobs = importJobQueryService.pageJobs(1L, new ImportJobPageQuery(0, 10, "FAILED", null, null, false));
        assertThat(failedJobs.items()).extracting(com.renda.merchantops.api.dto.importjob.query.ImportJobListItemResponse::id)
                .containsExactly(5102L, 5104L);

        ImportJobPageResponse requestedByJobs = importJobQueryService.pageJobs(1L, new ImportJobPageQuery(0, 10, null, null, 101L, false));
        assertThat(requestedByJobs.items()).extracting(com.renda.merchantops.api.dto.importjob.query.ImportJobListItemResponse::id)
                .containsExactly(5102L, 5101L, 5104L);

        ImportJobPageResponse failuresOnlyJobs = importJobQueryService.pageJobs(1L, new ImportJobPageQuery(0, 10, null, null, null, true));
        assertThat(failuresOnlyJobs.items()).extracting(com.renda.merchantops.api.dto.importjob.query.ImportJobListItemResponse::id)
                .containsExactly(5103L, 5102L, 5104L);
        assertThat(failuresOnlyJobs.items()).allSatisfy(item -> assertThat(item.hasFailures()).isTrue());

        ImportJobPageResponse userCsvJobs = importJobQueryService.pageJobs(1L, new ImportJobPageQuery(0, 10, null, "USER_CSV", null, false));
        assertThat(userCsvJobs.items()).extracting(com.renda.merchantops.api.dto.importjob.query.ImportJobListItemResponse::id)
                .containsExactly(5103L, 5102L, 5101L);
    }

    @Test
    void pageJobErrorsShouldApplyTenantScopeStableOrderingAndStayConsistentWithDetailSummary() {
        insertImportJob(6101L, 1L, "USER_CSV", "FAILED", 101L, 4, 0, 4, "all rows failed validation", "2026-03-12 10:00:00");
        insertImportJob(6201L, 2L, "USER_CSV", "FAILED", 201L, 1, 0, 1, "all rows failed validation", "2026-03-12 10:00:00");

        insertImportJobError(9101L, 1L, 6101L, null, "INVALID_HEADER", "header mismatch", "header", "2026-03-12 10:00:01");
        insertImportJobError(9102L, 1L, 6101L, 3, "DUPLICATE_USERNAME", "duplicate username", "row-3", "2026-03-12 10:00:02");
        insertImportJobError(9104L, 1L, 6101L, 5, "UNKNOWN_ROLE", "role missing", "row-5-b", "2026-03-12 10:00:04");
        insertImportJobError(9103L, 1L, 6101L, 5, "INVALID_EMAIL", "email invalid", "row-5-a", "2026-03-12 10:00:03");
        insertImportJobError(9201L, 2L, 6201L, 2, "UNKNOWN_ROLE", "role missing", "other-tenant", "2026-03-12 10:00:05");

        ImportJobErrorPageResponse firstPage = importJobQueryService.pageJobErrors(1L, 6101L, new ImportJobErrorPageQuery(0, 2, null));
        ImportJobErrorPageResponse secondPage = importJobQueryService.pageJobErrors(1L, 6101L, new ImportJobErrorPageQuery(1, 2, null));
        ImportJobErrorPageResponse filteredPage = importJobQueryService.pageJobErrors(1L, 6101L, new ImportJobErrorPageQuery(0, 10, "UNKNOWN_ROLE"));

        assertThat(firstPage.total()).isEqualTo(4);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.items()).extracting(com.renda.merchantops.api.dto.importjob.query.ImportJobErrorItemResponse::id)
                .containsExactly(9101L, 9102L);
        assertThat(secondPage.items()).extracting(com.renda.merchantops.api.dto.importjob.query.ImportJobErrorItemResponse::id)
                .containsExactly(9103L, 9104L);
        assertThat(filteredPage.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(9104L);
            assertThat(item.rowNumber()).isEqualTo(5);
        });

        ImportJobDetailResponse detail = importJobQueryService.getJobDetail(1L, 6101L);
        assertThat(detail.itemErrors()).extracting(com.renda.merchantops.api.dto.importjob.query.ImportJobErrorItemResponse::id)
                .containsExactly(9101L, 9102L, 9103L, 9104L);
        assertThat(toErrorCountMap(detail.errorCodeCounts())).isEqualTo(
                Stream.concat(firstPage.items().stream(), secondPage.items().stream())
                        .collect(Collectors.groupingBy(
                                com.renda.merchantops.api.dto.importjob.query.ImportJobErrorItemResponse::errorCode,
                                Collectors.counting()
                        ))
        );

        assertThatThrownBy(() -> importJobQueryService.pageJobErrors(1L, 6201L, new ImportJobErrorPageQuery(0, 10, null)))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    private void assertReplayRollbackCleansStagedFile(String requestId, Runnable replayInvocation) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        long baselineFileCount = countStoredFiles();
        AtomicLong fileCountInsideTransaction = new AtomicLong();
        transactionTemplate.executeWithoutResult(status -> {
            replayInvocation.run();
            fileCountInsideTransaction.set(countStoredFiles());
            status.setRollbackOnly();
        });
        assertThat(fileCountInsideTransaction.get()).isGreaterThan(baselineFileCount);
        assertThat(countStoredFiles()).isEqualTo(baselineFileCount);
        Integer replayJobCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM import_job WHERE tenant_id = 1 AND request_id = ?",
                Integer.class,
                requestId
        );
        assertThat(replayJobCount).isZero();
    }

    private long countStoredFiles() {
        if (!Files.exists(STORAGE_ROOT)) {
            return 0;
        }
        try (var walk = Files.walk(STORAGE_ROOT)) {
            return walk.filter(Files::isRegularFile).count();
        } catch (Exception ex) {
            throw new IllegalStateException("failed to inspect import storage", ex);
        }
    }

    private void insertImportJob(Long id,
                                 Long tenantId,
                                 String importType,
                                 String status,
                                 Long requestedBy,
                                 int totalCount,
                                 int successCount,
                                 int failureCount,
                                 String errorSummary,
                                 String createdAt) {
        jdbcTemplate.update("""
                INSERT INTO import_job (
                    id, tenant_id, import_type, source_type, source_filename, storage_key, status,
                    requested_by, request_id, total_count, success_count, failure_count, error_summary,
                    created_at, started_at, finished_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, PARSEDATETIME(?, 'yyyy-MM-dd HH:mm:ss'), NULL, NULL)
                """,
                id,
                tenantId,
                importType,
                "CSV",
                "users-" + id + ".csv",
                tenantId + "/" + id + ".csv",
                status,
                requestedBy,
                "req-" + id,
                totalCount,
                successCount,
                failureCount,
                errorSummary,
                createdAt
        );
    }

    private void insertImportJobError(Long id,
                                      Long tenantId,
                                      Long importJobId,
                                      Integer rowNumber,
                                      String errorCode,
                                      String errorMessage,
                                      String rawPayload,
                                      String createdAt) {
        jdbcTemplate.update("""
                INSERT INTO import_job_item_error (
                    id, tenant_id, import_job_id, source_row_number, error_code, error_message, raw_payload, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, PARSEDATETIME(?, 'yyyy-MM-dd HH:mm:ss'))
                """,
                id,
                tenantId,
                importJobId,
                rowNumber,
                errorCode,
                errorMessage,
                rawPayload,
                createdAt
        );
    }

    private Map<String, Long> toErrorCountMap(java.util.List<ImportJobErrorCodeCountResponse> counts) {
        return counts.stream()
                .collect(Collectors.toMap(ImportJobErrorCodeCountResponse::errorCode, ImportJobErrorCodeCountResponse::count));
    }
}
