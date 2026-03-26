package com.renda.merchantops.api.importjob.messaging;

import com.renda.merchantops.api.audit.AuditEventService;
import com.renda.merchantops.api.config.ImportProcessingProperties;
import com.renda.merchantops.domain.importjob.ImportJobCommandPort;
import com.renda.merchantops.domain.importjob.ImportJobErrorRecord;
import com.renda.merchantops.domain.importjob.ImportJobRecord;
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
import static org.mockito.ArgumentMatchers.any;
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
    private ImportJobCommandPort importJobCommandPort;
    @Mock
    private AuditEventService auditEventService;
    @Mock
    private UserCsvImportProcessor userCsvImportProcessor;

    private ImportJobExecutionCoordinator importJobExecutionCoordinator;

    @BeforeEach
    void setUp() {
        ImportProcessingProperties importProcessingProperties = new ImportProcessingProperties();
        importProcessingProperties.setStaleProcessingThresholdSeconds(300);
        ImportJobFailureRecorder importJobFailureRecorder = new ImportJobFailureRecorder(importJobCommandPort, auditEventService);
        ImportJobChunkProcessor importJobChunkProcessor = new ImportJobChunkProcessor(importJobCommandPort, userCsvImportProcessor, importJobFailureRecorder);
        importJobExecutionCoordinator = new ImportJobExecutionCoordinator(
                importJobCommandPort,
                auditEventService,
                importJobChunkProcessor,
                importJobFailureRecorder,
                importProcessingProperties
        );
    }

    @Test
    void startProcessingShouldRequeueFreshProcessingJobs() {
        ImportJobRecord job = importJob(7001L, "PROCESSING", LocalDateTime.now(), 0, 0, 0);
        when(importJobCommandPort.findJobForUpdate(1L, 7001L)).thenReturn(Optional.of(job));

        ImportJobStartResult result = importJobExecutionCoordinator.startProcessing(7001L, 1L);

        assertThat(result.action()).isEqualTo(ImportJobStartAction.REQUEUE);
        assertThat(result.context()).isNull();
        verify(importJobCommandPort, never()).saveJob(any());
        verifyNoInteractions(auditEventService, userCsvImportProcessor);
    }

    @Test
    void processChunkShouldPersistHandledProgressBeforeUnexpectedRuntime() throws Exception {
        Method processChunk = ImportJobExecutionCoordinator.class.getMethod(
                "processChunk",
                ImportJobExecutionContext.class,
                List.class
        );
        Transactional transactional = processChunk.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.noRollbackFor()).contains(RuntimeException.class);

        ImportJobRecord job = importJob(7002L, "PROCESSING", LocalDateTime.now(), 5, 2, 1);
        when(importJobCommandPort.findJobForUpdate(1L, 7002L)).thenReturn(Optional.of(job));
        when(importJobCommandPort.saveJob(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ImportJobExecutionContext context = new ImportJobExecutionContext(7002L, 1L, "USER_CSV", "1/key.csv", 101L, "req-1");
        doAnswer(invocation -> {
            int rowNumber = invocation.getArgument(1, Integer.class);
            if (rowNumber == 3) {
                throw new ImportRowProcessingException("INVALID_EMAIL", "email format is invalid");
            }
            if (rowNumber == 4) {
                throw new IllegalStateException("synthetic row crash");
            }
            return null;
        }).when(userCsvImportProcessor).processRow(eq(context), anyInt(), anyList());

        List<ImportJobChunkRow> rows = List.of(
                new ImportJobChunkRow(2, validColumns(), "alpha,Alpha User,alpha@example.com,abc123,READ_ONLY"),
                new ImportJobChunkRow(3, validColumns(), "invalid,Invalid User,not-an-email,abc123,READ_ONLY"),
                new ImportJobChunkRow(4, validColumns(), "omega,Omega User,omega@example.com,abc123,READ_ONLY")
        );

        assertThatThrownBy(() -> importJobExecutionCoordinator.processChunk(context, rows))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("synthetic row crash");

        ArgumentCaptor<ImportJobRecord> jobCaptor = ArgumentCaptor.forClass(ImportJobRecord.class);
        verify(importJobCommandPort).saveJob(jobCaptor.capture());
        assertThat(jobCaptor.getValue().totalCount()).isEqualTo(8);
        assertThat(jobCaptor.getValue().successCount()).isEqualTo(3);
        assertThat(jobCaptor.getValue().failureCount()).isEqualTo(2);

        ArgumentCaptor<ImportJobErrorRecord> errorCaptor = ArgumentCaptor.forClass(ImportJobErrorRecord.class);
        verify(importJobCommandPort).saveJobError(errorCaptor.capture());
        assertThat(errorCaptor.getValue().importJobId()).isEqualTo(7002L);
        assertThat(errorCaptor.getValue().rowNumber()).isEqualTo(3);
        assertThat(errorCaptor.getValue().errorCode()).isEqualTo("INVALID_EMAIL");
    }

    private ImportJobRecord importJob(Long id,
                                      String status,
                                      LocalDateTime startedAt,
                                      int totalCount,
                                      int successCount,
                                      int failureCount) {
        return new ImportJobRecord(
                id,
                1L,
                "USER_CSV",
                "CSV",
                "users.csv",
                "1/key.csv",
                null,
                status,
                101L,
                "req-1",
                totalCount,
                successCount,
                failureCount,
                null,
                LocalDateTime.now().minusMinutes(5),
                startedAt,
                null
        );
    }

    private List<String> validColumns() {
        return List.of("alpha", "Alpha User", "alpha@example.com", "abc123", "READ_ONLY");
    }
}
