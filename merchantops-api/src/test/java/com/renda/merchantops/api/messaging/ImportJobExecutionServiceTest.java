package com.renda.merchantops.api.messaging;

import com.renda.merchantops.api.config.ImportProcessingProperties;
import com.renda.merchantops.api.service.AuditEventService;
import com.renda.merchantops.infra.persistence.entity.ImportJobEntity;
import com.renda.merchantops.infra.persistence.entity.ImportJobItemErrorEntity;
import com.renda.merchantops.infra.repository.ImportJobItemErrorRepository;
import com.renda.merchantops.infra.repository.ImportJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportJobExecutionServiceTest {

    @Mock
    private ImportJobRepository importJobRepository;
    @Mock
    private ImportJobItemErrorRepository importJobItemErrorRepository;
    @Mock
    private AuditEventService auditEventService;
    @Mock
    private UserCsvImportProcessor userCsvImportProcessor;

    private ImportJobExecutionService importJobExecutionService;

    @BeforeEach
    void setUp() {
        ImportProcessingProperties importProcessingProperties = new ImportProcessingProperties();
        importProcessingProperties.setStaleProcessingThresholdSeconds(300);
        importJobExecutionService = new ImportJobExecutionService(
                importJobRepository,
                importJobItemErrorRepository,
                auditEventService,
                userCsvImportProcessor,
                importProcessingProperties
        );
    }

    @Test
    void startProcessingShouldRequeueFreshProcessingJobs() {
        ImportJobEntity job = new ImportJobEntity();
        job.setId(7001L);
        job.setTenantId(1L);
        job.setStatus("PROCESSING");
        job.setStartedAt(LocalDateTime.now());

        when(importJobRepository.findByIdAndTenantIdForUpdate(7001L, 1L)).thenReturn(Optional.of(job));

        ImportJobExecutionService.ImportJobStartResult result = importJobExecutionService.startProcessing(7001L, 1L);

        assertThat(result.action()).isEqualTo(ImportJobExecutionService.ImportJobStartAction.REQUEUE);
        assertThat(result.context()).isNull();
        verify(importJobRepository, never()).save(job);
        verifyNoInteractions(auditEventService, importJobItemErrorRepository, userCsvImportProcessor);
    }

    @Test
    void processChunkShouldPersistHandledProgressBeforeUnexpectedRuntime() throws Exception {
        Method processChunk = ImportJobExecutionService.class.getMethod(
                "processChunk",
                ImportJobExecutionService.ImportJobExecutionContext.class,
                List.class
        );
        Transactional transactional = processChunk.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.noRollbackFor()).contains(RuntimeException.class);

        ImportJobEntity job = new ImportJobEntity();
        job.setId(7002L);
        job.setTenantId(1L);
        job.setImportType("USER_CSV");
        job.setTotalCount(5);
        job.setSuccessCount(2);
        job.setFailureCount(1);

        when(importJobRepository.findByIdAndTenantIdForUpdate(7002L, 1L)).thenReturn(Optional.of(job));
        doAnswer(invocation -> {
            int rowNumber = invocation.getArgument(1, Integer.class);
            if (rowNumber == 3) {
                throw new ImportRowProcessingException("INVALID_EMAIL", "email format is invalid");
            }
            if (rowNumber == 4) {
                throw new IllegalStateException("synthetic row crash");
            }
            return null;
        }).when(userCsvImportProcessor).processRow(eq(job), anyInt(), anyList());

        ImportJobExecutionService.ImportJobExecutionContext context =
                new ImportJobExecutionService.ImportJobExecutionContext(7002L, 1L, "USER_CSV", "1/key.csv", 101L, "req-1");
        List<ImportJobExecutionService.ImportJobChunkRow> rows = List.of(
                new ImportJobExecutionService.ImportJobChunkRow(2, validColumns(), "alpha,Alpha User,alpha@example.com,abc123,READ_ONLY"),
                new ImportJobExecutionService.ImportJobChunkRow(3, validColumns(), "invalid,Invalid User,not-an-email,abc123,READ_ONLY"),
                new ImportJobExecutionService.ImportJobChunkRow(4, validColumns(), "omega,Omega User,omega@example.com,abc123,READ_ONLY")
        );

        assertThatThrownBy(() -> importJobExecutionService.processChunk(context, rows))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("synthetic row crash");

        assertThat(job.getTotalCount()).isEqualTo(8);
        assertThat(job.getSuccessCount()).isEqualTo(3);
        assertThat(job.getFailureCount()).isEqualTo(2);
        verify(importJobRepository).save(job);

        ArgumentCaptor<ImportJobItemErrorEntity> errorCaptor = ArgumentCaptor.forClass(ImportJobItemErrorEntity.class);
        verify(importJobItemErrorRepository).save(errorCaptor.capture());
        assertThat(errorCaptor.getValue().getImportJobId()).isEqualTo(7002L);
        assertThat(errorCaptor.getValue().getRowNumber()).isEqualTo(3);
        assertThat(errorCaptor.getValue().getErrorCode()).isEqualTo("INVALID_EMAIL");
    }

    private List<String> validColumns() {
        return List.of("alpha", "Alpha User", "alpha@example.com", "abc123", "READ_ONLY");
    }
}
