package com.renda.merchantops.api.messaging;

import com.renda.merchantops.api.service.AuditEventService;
import com.renda.merchantops.api.service.ImportFileStorageService;
import com.renda.merchantops.infra.persistence.entity.ImportJobEntity;
import com.renda.merchantops.infra.repository.ImportJobItemErrorRepository;
import com.renda.merchantops.infra.repository.ImportJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportJobWorkerTest {

    @Mock
    private ImportJobRepository importJobRepository;
    @Mock
    private ImportJobItemErrorRepository importJobItemErrorRepository;
    @Mock
    private ImportFileStorageService importFileStorageService;
    @Mock
    private AuditEventService auditEventService;
    @Mock
    private UserCsvImportProcessor userCsvImportProcessor;

    @InjectMocks
    private ImportJobWorker importJobWorker;

    @Test
    void consumeShouldRunBusinessProcessorPerValidRow() throws Exception {
        ImportJobEntity job = new ImportJobEntity();
        job.setId(7001L);
        job.setTenantId(1L);
        job.setImportType("USER_CSV");
        job.setStatus("QUEUED");
        job.setStorageKey("1/key.csv");
        job.setRequestedBy(101L);
        job.setRequestId("req-1");
        when(importJobRepository.findByIdAndTenantIdForUpdate(7001L, 1L)).thenReturn(Optional.of(job));
        when(importJobRepository.save(any(ImportJobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importFileStorageService.openStream("1/key.csv")).thenReturn(new ByteArrayInputStream(
                "username,displayName,email,password,roleCodes\nvalid,Valid User,valid@example.com,123456,READ_ONLY\ninvalid-row".getBytes(StandardCharsets.UTF_8)
        ));

        importJobWorker.consume(new ImportJobMessage(7001L, 1L));

        verify(importFileStorageService).openStream("1/key.csv");
        verify(userCsvImportProcessor).processRow(any(), any(Integer.class), any());
        verify(importJobItemErrorRepository).save(any());
        assertThat(job.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(job.getSuccessCount()).isEqualTo(1);
        assertThat(job.getFailureCount()).isEqualTo(1);
    }

    @Test
    void consumeShouldFailUnsupportedImportTypeBeforeRowProcessing() throws Exception {
        ImportJobEntity job = new ImportJobEntity();
        job.setId(7002L);
        job.setTenantId(1L);
        job.setImportType("TICKET_CSV");
        job.setStatus("QUEUED");
        job.setStorageKey("1/key.csv");
        job.setRequestedBy(101L);
        job.setRequestId("req-2");
        when(importJobRepository.findByIdAndTenantIdForUpdate(7002L, 1L)).thenReturn(Optional.of(job));
        when(importJobRepository.save(any(ImportJobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importFileStorageService.openStream("1/key.csv")).thenReturn(new ByteArrayInputStream(
                "username,displayName,email,password,roleCodes\nvalid,Valid User,valid@example.com,123456,READ_ONLY".getBytes(StandardCharsets.UTF_8)
        ));

        importJobWorker.consume(new ImportJobMessage(7002L, 1L));

        verify(userCsvImportProcessor, never()).processRow(any(), any(Integer.class), any());
        assertThat(job.getStatus()).isEqualTo("FAILED");
        assertThat(job.getFailureCount()).isEqualTo(1);
    }
}
