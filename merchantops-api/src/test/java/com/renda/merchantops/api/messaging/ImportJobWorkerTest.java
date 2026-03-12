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

    @InjectMocks
    private ImportJobWorker importJobWorker;

    @Test
    void consumeShouldReadFileThroughStorageAbstraction() throws Exception {
        ImportJobEntity job = new ImportJobEntity();
        job.setId(7001L);
        job.setTenantId(1L);
        job.setStatus("QUEUED");
        job.setStorageKey("1/key.csv");
        job.setRequestedBy(101L);
        job.setRequestId("req-1");
        when(importJobRepository.findByIdAndTenantIdForUpdate(7001L, 1L)).thenReturn(Optional.of(job));
        when(importJobRepository.save(any(ImportJobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importFileStorageService.openStream("1/key.csv")).thenReturn(new ByteArrayInputStream(
                "username,email\nvalid,user@example.com\ninvalid-row".getBytes(StandardCharsets.UTF_8)
        ));

        importJobWorker.consume(new ImportJobMessage(7001L, 1L));

        verify(importFileStorageService).openStream("1/key.csv");
        verify(importJobItemErrorRepository).save(any());
        assertThat(job.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(job.getSuccessCount()).isEqualTo(1);
        assertThat(job.getFailureCount()).isEqualTo(1);
    }
}
