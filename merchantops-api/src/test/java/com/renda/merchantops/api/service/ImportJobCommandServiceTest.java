package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.importjob.command.ImportJobCreateRequest;
import com.renda.merchantops.api.dto.importjob.query.ImportJobDetailResponse;
import com.renda.merchantops.api.messaging.ImportJobCreatedEvent;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.ImportJobEntity;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.repository.ImportJobRepository;
import com.renda.merchantops.infra.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ImportJobCommandServiceTest {

    @Mock
    private ImportJobRepository importJobRepository;
    @Mock
    private ImportFileStorageService importFileStorageService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private ImportJobQueryService importJobQueryService;
    @Mock
    private ImportReplayFileBuilder importReplayFileBuilder;
    @Mock
    private AuditEventService auditEventService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ImportJobCommandService importJobCommandService;

    @Test
    void createJobShouldPersistQueuedJobAndPublishImportEvent() {
        UserEntity user = new UserEntity();
        user.setId(101L);
        user.setTenantId(1L);
        when(userRepository.findByIdAndTenantId(101L, 1L)).thenReturn(Optional.of(user));
        when(importFileStorageService.store(any(), any())).thenReturn("1/key.csv");
        when(importJobRepository.save(any(ImportJobEntity.class))).thenAnswer(invocation -> {
            ImportJobEntity entity = invocation.getArgument(0);
            entity.setId(7001L);
            return entity;
        });
        when(importJobQueryService.toDetail(any())).thenReturn(new ImportJobDetailResponse(
                7001L, 1L, "USER_CSV", "CSV", "users.csv", "1/key.csv", null, "QUEUED", 101L, "req-1",
                0, 0, 0, null, null, null, null, List.of(), List.of()
        ));

        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("user_csv");
        MockMultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv", "name,email\na,b".getBytes());

        ImportJobDetailResponse response = importJobCommandService.createJob(1L, 101L, "req-1", request, file);

        assertThat(response.id()).isEqualTo(7001L);
        ArgumentCaptor<ImportJobEntity> captor = ArgumentCaptor.forClass(ImportJobEntity.class);
        verify(importJobRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("QUEUED");
        assertThat(captor.getValue().getImportType()).isEqualTo("USER_CSV");
        verify(applicationEventPublisher).publishEvent(new ImportJobCreatedEvent(7001L, 1L));
    }

    @Test
    void createJobShouldRejectInvalidImportType() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("ticket_csv");
        MockMultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv", "a".getBytes());

        assertThatThrownBy(() -> importJobCommandService.createJob(1L, 101L, "req-1", request, file))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                });
    }

    @Test
    void replayFailedRowsShouldCreateDerivedQueuedJobWithSourceLineage() {
        UserEntity user = new UserEntity();
        user.setId(101L);
        user.setTenantId(1L);
        ImportJobEntity sourceJob = new ImportJobEntity();
        sourceJob.setId(7001L);
        sourceJob.setTenantId(1L);
        sourceJob.setImportType("USER_CSV");
        sourceJob.setSourceType("CSV");
        sourceJob.setSourceFilename("users.csv");
        sourceJob.setStorageKey("1/original.csv");
        sourceJob.setStatus("SUCCEEDED");
        sourceJob.setRequestedBy(101L);
        sourceJob.setRequestId("req-source");
        sourceJob.setFailureCount(1);
        when(userRepository.findByIdAndTenantId(101L, 1L)).thenReturn(Optional.of(user));
        when(importReplayFileBuilder.buildFailedRowReplay(1L, 7001L)).thenReturn(
                new ImportReplayFileBuilder.ReplayFileBuildResult(
                        sourceJob,
                        "replay-failures-job-7001.csv",
                        "1/replay.csv",
                        1
                )
        );
        when(importJobRepository.save(any(ImportJobEntity.class))).thenAnswer(invocation -> {
            ImportJobEntity entity = invocation.getArgument(0);
            entity.setId(7002L);
            return entity;
        });
        when(importJobQueryService.toDetail(any())).thenReturn(new ImportJobDetailResponse(
                7002L, 1L, "USER_CSV", "CSV", "replay-failures-job-7001.csv", "1/replay.csv", 7001L,
                "QUEUED", 101L, "req-replay-1", 0, 0, 0, null, null, null, null, List.of(), List.of()
        ));

        ImportJobDetailResponse response = importJobCommandService.replayFailedRows(1L, 101L, "req-replay-1", 7001L);

        assertThat(response.id()).isEqualTo(7002L);
        assertThat(response.sourceJobId()).isEqualTo(7001L);
        ArgumentCaptor<ImportJobEntity> jobCaptor = ArgumentCaptor.forClass(ImportJobEntity.class);
        verify(importJobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getSourceJobId()).isEqualTo(7001L);
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo("QUEUED");
        verify(applicationEventPublisher).publishEvent(new ImportJobCreatedEvent(7002L, 1L));

        ArgumentCaptor<Long> entityIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> afterCaptor = ArgumentCaptor.forClass(Object.class);
        verify(auditEventService, times(2)).recordEvent(
                eq(1L),
                eq("IMPORT_JOB"),
                entityIdCaptor.capture(),
                actionCaptor.capture(),
                eq(101L),
                eq("req-replay-1"),
                isNull(),
                afterCaptor.capture()
        );
        assertThat(actionCaptor.getAllValues()).containsExactly("IMPORT_JOB_REPLAY_REQUESTED", "IMPORT_JOB_CREATED");
        assertThat(entityIdCaptor.getAllValues()).containsExactly(7001L, 7002L);
        @SuppressWarnings("unchecked")
        Map<String, Object> replayRequestedAfter = (Map<String, Object>) afterCaptor.getAllValues().get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> createdAfter = (Map<String, Object>) afterCaptor.getAllValues().get(1);
        assertThat(replayRequestedAfter).containsEntry("replayJobId", 7002L);
        assertThat(createdAfter).containsEntry("sourceJobId", 7001L);
    }
}
