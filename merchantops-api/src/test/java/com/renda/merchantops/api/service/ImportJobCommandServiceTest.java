package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.importjob.command.ImportJobCreateRequest;
import com.renda.merchantops.api.dto.importjob.command.ImportJobEditedReplayItemRequest;
import com.renda.merchantops.api.dto.importjob.command.ImportJobEditedReplayRequest;
import com.renda.merchantops.api.dto.importjob.command.ImportJobSelectiveReplayRequest;
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
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Test
    void replayWholeFileShouldCreateDerivedQueuedJobWithWholeFileAuditMetadata() {
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
        sourceJob.setStatus("FAILED");
        sourceJob.setRequestedBy(101L);
        sourceJob.setRequestId("req-source");
        sourceJob.setTotalCount(3);
        sourceJob.setSuccessCount(0);
        sourceJob.setFailureCount(3);
        when(userRepository.findByIdAndTenantId(101L, 1L)).thenReturn(Optional.of(user));
        when(importReplayFileBuilder.buildWholeFileReplay(1L, 7001L)).thenReturn(
                new ImportReplayFileBuilder.ReplayFileBuildResult(
                        sourceJob,
                        "replay-file-job-7001.csv",
                        "1/replay-file.csv",
                        3
                )
        );
        when(importJobRepository.save(any(ImportJobEntity.class))).thenAnswer(invocation -> {
            ImportJobEntity entity = invocation.getArgument(0);
            entity.setId(7005L);
            return entity;
        });
        when(importJobQueryService.toDetail(any())).thenReturn(new ImportJobDetailResponse(
                7005L, 1L, "USER_CSV", "CSV", "replay-file-job-7001.csv", "1/replay-file.csv", 7001L,
                "QUEUED", 101L, "req-replay-file-1", 0, 0, 0, null, null, null, null, List.of(), List.of()
        ));

        ImportJobDetailResponse response = importJobCommandService.replayWholeFile(1L, 101L, "req-replay-file-1", 7001L);

        assertThat(response.id()).isEqualTo(7005L);
        assertThat(response.sourceJobId()).isEqualTo(7001L);
        verify(importReplayFileBuilder).buildWholeFileReplay(1L, 7001L);
        verify(applicationEventPublisher).publishEvent(new ImportJobCreatedEvent(7005L, 1L));

        ArgumentCaptor<Long> entityIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> afterCaptor = ArgumentCaptor.forClass(Object.class);
        verify(auditEventService, times(2)).recordEvent(
                eq(1L),
                eq("IMPORT_JOB"),
                entityIdCaptor.capture(),
                actionCaptor.capture(),
                eq(101L),
                eq("req-replay-file-1"),
                isNull(),
                afterCaptor.capture()
        );
        assertThat(actionCaptor.getAllValues()).containsExactly("IMPORT_JOB_REPLAY_REQUESTED", "IMPORT_JOB_CREATED");
        assertThat(entityIdCaptor.getAllValues()).containsExactly(7001L, 7005L);
        @SuppressWarnings("unchecked")
        Map<String, Object> replayRequestedAfter = (Map<String, Object>) afterCaptor.getAllValues().get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> createdAfter = (Map<String, Object>) afterCaptor.getAllValues().get(1);
        assertThat(replayRequestedAfter).containsEntry("replayJobId", 7005L);
        assertThat(replayRequestedAfter).containsEntry("replayedFailureCount", 3);
        assertThat(replayRequestedAfter).containsEntry("replayMode", "WHOLE_FILE");
        assertThat(createdAfter).containsEntry("sourceJobId", 7001L);
        assertThat(createdAfter).containsEntry("replayMode", "WHOLE_FILE");
    }

    @Test
    void replayFailedRowsSelectiveShouldNormalizeCodesAndPersistAuditSelection() {
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
        sourceJob.setFailureCount(2);
        when(userRepository.findByIdAndTenantId(101L, 1L)).thenReturn(Optional.of(user));
        when(importReplayFileBuilder.buildSelectiveFailedRowReplay(1L, 7001L, List.of("UNKNOWN_ROLE", "INVALID_EMAIL"))).thenReturn(
                new ImportReplayFileBuilder.ReplayFileBuildResult(
                        sourceJob,
                        "replay-failures-job-7001.csv",
                        "1/replay-selective.csv",
                        1
                )
        );
        when(importJobRepository.save(any(ImportJobEntity.class))).thenAnswer(invocation -> {
            ImportJobEntity entity = invocation.getArgument(0);
            entity.setId(7003L);
            return entity;
        });
        when(importJobQueryService.toDetail(any())).thenReturn(new ImportJobDetailResponse(
                7003L, 1L, "USER_CSV", "CSV", "replay-failures-job-7001.csv", "1/replay-selective.csv", 7001L,
                "QUEUED", 101L, "req-replay-selective-1", 0, 0, 0, null, null, null, null, List.of(), List.of()
        ));

        ImportJobSelectiveReplayRequest request = new ImportJobSelectiveReplayRequest(List.of(" UNKNOWN_ROLE ", "INVALID_EMAIL", "UNKNOWN_ROLE"));

        ImportJobDetailResponse response = importJobCommandService.replayFailedRowsSelective(
                1L,
                101L,
                "req-replay-selective-1",
                7001L,
                request
        );

        assertThat(response.id()).isEqualTo(7003L);
        assertThat(response.sourceJobId()).isEqualTo(7001L);
        verify(importReplayFileBuilder).buildSelectiveFailedRowReplay(1L, 7001L, List.of("UNKNOWN_ROLE", "INVALID_EMAIL"));
        verify(applicationEventPublisher).publishEvent(new ImportJobCreatedEvent(7003L, 1L));

        ArgumentCaptor<Long> entityIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> afterCaptor = ArgumentCaptor.forClass(Object.class);
        verify(auditEventService, times(2)).recordEvent(
                eq(1L),
                eq("IMPORT_JOB"),
                entityIdCaptor.capture(),
                actionCaptor.capture(),
                eq(101L),
                eq("req-replay-selective-1"),
                isNull(),
                afterCaptor.capture()
        );
        assertThat(actionCaptor.getAllValues()).containsExactly("IMPORT_JOB_REPLAY_REQUESTED", "IMPORT_JOB_CREATED");
        assertThat(entityIdCaptor.getAllValues()).containsExactly(7001L, 7003L);
        @SuppressWarnings("unchecked")
        Map<String, Object> replayRequestedAfter = (Map<String, Object>) afterCaptor.getAllValues().get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> createdAfter = (Map<String, Object>) afterCaptor.getAllValues().get(1);
        assertThat(replayRequestedAfter).containsEntry("replayJobId", 7003L);
        assertThat(replayRequestedAfter).containsEntry("selectedErrorCodes", List.of("UNKNOWN_ROLE", "INVALID_EMAIL"));
        assertThat(createdAfter).containsEntry("sourceJobId", 7001L);
        assertThat(createdAfter).containsEntry("selectedErrorCodes", List.of("UNKNOWN_ROLE", "INVALID_EMAIL"));
    }

    @Test
    void replayFailedRowsSelectiveShouldRejectEmptyErrorCodes() {
        UserEntity user = new UserEntity();
        user.setId(101L);
        user.setTenantId(1L);
        when(userRepository.findByIdAndTenantId(101L, 1L)).thenReturn(Optional.of(user));

        ImportJobSelectiveReplayRequest request = new ImportJobSelectiveReplayRequest(List.of());

        assertThatThrownBy(() -> importJobCommandService.replayFailedRowsSelective(1L, 101L, "req-replay-selective-invalid", 7001L, request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));

        verifyNoInteractions(importReplayFileBuilder);
    }

    @Test
    void replayFailedRowsEditedShouldNormalizeRowsAndPersistScopeOnlyAuditMetadata() {
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

        List<ImportReplayFileBuilder.EditedReplayRowReplacement> normalizedRows = List.of(
                new ImportReplayFileBuilder.EditedReplayRowReplacement(
                        701L,
                        "retry-user",
                        "Retry User",
                        "retry-user@demo-shop.local",
                        " 123456 ",
                        List.of("READ_ONLY", "TENANT_ADMIN")
                )
        );
        when(importReplayFileBuilder.buildEditedFailedRowReplay(1L, 7001L, normalizedRows)).thenReturn(
                new ImportReplayFileBuilder.ReplayFileBuildResult(
                        sourceJob,
                        "replay-edited-job-7001.csv",
                        "1/replay-edited.csv",
                        1
                )
        );
        when(importJobRepository.save(any(ImportJobEntity.class))).thenAnswer(invocation -> {
            ImportJobEntity entity = invocation.getArgument(0);
            entity.setId(7004L);
            return entity;
        });
        when(importJobQueryService.toDetail(any())).thenReturn(new ImportJobDetailResponse(
                7004L, 1L, "USER_CSV", "CSV", "replay-edited-job-7001.csv", "1/replay-edited.csv", 7001L,
                "QUEUED", 101L, "req-replay-edited-1", 0, 0, 0, null, null, null, null, List.of(), List.of()
        ));

        ImportJobEditedReplayRequest request = new ImportJobEditedReplayRequest(List.of(
                new ImportJobEditedReplayItemRequest(
                        701L,
                        " retry-user ",
                        " Retry User ",
                        " retry-user@demo-shop.local ",
                        " 123456 ",
                        List.of(" READ_ONLY ", "TENANT_ADMIN", "READ_ONLY")
                )
        ));

        ImportJobDetailResponse response = importJobCommandService.replayFailedRowsEdited(
                1L,
                101L,
                "req-replay-edited-1",
                7001L,
                request
        );

        assertThat(response.id()).isEqualTo(7004L);
        assertThat(response.sourceJobId()).isEqualTo(7001L);
        verify(importReplayFileBuilder).buildEditedFailedRowReplay(1L, 7001L, normalizedRows);
        verify(applicationEventPublisher).publishEvent(new ImportJobCreatedEvent(7004L, 1L));

        ArgumentCaptor<Long> entityIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> afterCaptor = ArgumentCaptor.forClass(Object.class);
        verify(auditEventService, times(2)).recordEvent(
                eq(1L),
                eq("IMPORT_JOB"),
                entityIdCaptor.capture(),
                actionCaptor.capture(),
                eq(101L),
                eq("req-replay-edited-1"),
                isNull(),
                afterCaptor.capture()
        );
        assertThat(actionCaptor.getAllValues()).containsExactly("IMPORT_JOB_REPLAY_REQUESTED", "IMPORT_JOB_CREATED");
        assertThat(entityIdCaptor.getAllValues()).containsExactly(7001L, 7004L);
        @SuppressWarnings("unchecked")
        Map<String, Object> replayRequestedAfter = (Map<String, Object>) afterCaptor.getAllValues().get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> createdAfter = (Map<String, Object>) afterCaptor.getAllValues().get(1);
        assertThat(replayRequestedAfter).containsEntry("replayJobId", 7004L);
        assertThat(replayRequestedAfter).containsEntry("editedErrorIds", List.of(701L));
        assertThat(replayRequestedAfter).containsEntry("editedRowCount", 1);
        assertThat(replayRequestedAfter).containsEntry("editedFields", List.of("username", "displayName", "email", "password", "roleCodes"));
        assertThat(createdAfter).containsEntry("sourceJobId", 7001L);
        assertThat(createdAfter).containsEntry("editedErrorIds", List.of(701L));
        assertThat(createdAfter).containsEntry("editedRowCount", 1);
        assertThat(createdAfter).containsEntry("editedFields", List.of("username", "displayName", "email", "password", "roleCodes"));
        assertThat(replayRequestedAfter.toString()).doesNotContain("retry-user@demo-shop.local").doesNotContain(" 123456 ");
        assertThat(createdAfter.toString()).doesNotContain("retry-user@demo-shop.local").doesNotContain(" 123456 ");
    }

    @Test
    void replayFailedRowsEditedShouldRejectDuplicateErrorIds() {
        UserEntity user = new UserEntity();
        user.setId(101L);
        user.setTenantId(1L);
        when(userRepository.findByIdAndTenantId(101L, 1L)).thenReturn(Optional.of(user));

        ImportJobEditedReplayRequest request = new ImportJobEditedReplayRequest(List.of(
                new ImportJobEditedReplayItemRequest(701L, "retry-a", "Retry A", "retry-a@demo-shop.local", "123456", List.of("READ_ONLY")),
                new ImportJobEditedReplayItemRequest(701L, "retry-b", "Retry B", "retry-b@demo-shop.local", "123456", List.of("READ_ONLY"))
        ));

        assertThatThrownBy(() -> importJobCommandService.replayFailedRowsEdited(1L, 101L, "req-replay-edited-dup", 7001L, request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(biz.getMessage()).contains("duplicate errorId");
                });

        verifyNoInteractions(importReplayFileBuilder);
    }
}
