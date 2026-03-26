package com.renda.merchantops.api.importjob;

import com.renda.merchantops.api.dto.importjob.command.ImportJobCreateRequest;
import com.renda.merchantops.api.dto.importjob.command.ImportJobEditedReplayItemRequest;
import com.renda.merchantops.api.dto.importjob.command.ImportJobEditedReplayRequest;
import com.renda.merchantops.api.dto.importjob.command.ImportJobSelectiveReplayRequest;
import com.renda.merchantops.api.dto.importjob.query.ImportJobDetailResponse;
import com.renda.merchantops.api.importjob.messaging.ImportJobCreatedEvent;
import com.renda.merchantops.api.importjob.replay.ImportReplayFileWriter;
import com.renda.merchantops.api.importjob.replay.ImportReplaySourceLoader;
import com.renda.merchantops.domain.importjob.ImportJobCommandUseCase;
import com.renda.merchantops.domain.importjob.ImportJobErrorRecord;
import com.renda.merchantops.domain.importjob.ImportJobRecord;
import com.renda.merchantops.domain.importjob.NewImportJobDraft;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportJobCommandServiceTest {

    @Mock
    private ImportJobCommandUseCase importJobCommandUseCase;
    @Mock
    private ImportFileStorageService importFileStorageService;
    @Mock
    private ImportJobOperatorValidator importJobOperatorValidator;
    @Mock
    private ImportStoredFileCleanupSupport importStoredFileCleanupSupport;
    @Mock
    private ImportJobQueryService importJobQueryService;
    @Mock
    private ImportJobAuditService importJobAuditService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private ImportReplaySourceLoader importReplaySourceLoader;
    @Mock
    private ImportReplayFileWriter importReplayFileWriter;

    private ImportJobSubmissionService importJobSubmissionService;
    private ImportJobReplayService importJobReplayService;

    @BeforeEach
    void setUp() {
        importJobSubmissionService = new ImportJobSubmissionService(
                importJobCommandUseCase,
                importFileStorageService,
                importJobOperatorValidator,
                importStoredFileCleanupSupport,
                importJobQueryService,
                importJobAuditService,
                applicationEventPublisher
        );
        importJobReplayService = new ImportJobReplayService(
                importJobCommandUseCase,
                importJobOperatorValidator,
                importStoredFileCleanupSupport,
                importJobQueryService,
                importJobAuditService,
                importReplaySourceLoader,
                importReplayFileWriter,
                applicationEventPublisher
        );
    }

    @Test
    void createJobShouldPersistQueuedJobAndPublishImportEvent() {
        when(importFileStorageService.store(any(), any(MockMultipartFile.class))).thenReturn("1/key.csv");
        when(importJobCommandUseCase.createQueuedJob(eq(1L), eq(101L), eq("req-1"), any(NewImportJobDraft.class)))
                .thenReturn(jobRecord(7001L, null, "users.csv", "1/key.csv"));
        when(importJobQueryService.getJobDetail(1L, 7001L)).thenReturn(detailResponse(7001L, null, "users.csv", "1/key.csv", "req-1"));

        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("USER_CSV");
        MockMultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv", "name,email\na,b".getBytes());

        ImportJobDetailResponse response = importJobSubmissionService.createJob(1L, 101L, "req-1", request, file);

        assertThat(response.id()).isEqualTo(7001L);
        ArgumentCaptor<NewImportJobDraft> draftCaptor = ArgumentCaptor.forClass(NewImportJobDraft.class);
        verify(importJobCommandUseCase).createQueuedJob(eq(1L), eq(101L), eq("req-1"), draftCaptor.capture());
        assertThat(draftCaptor.getValue().importType()).isEqualTo("USER_CSV");
        assertThat(draftCaptor.getValue().sourceType()).isEqualTo("CSV");
        assertThat(draftCaptor.getValue().sourceFilename()).isEqualTo("users.csv");
        assertThat(draftCaptor.getValue().storageKey()).isEqualTo("1/key.csv");
        verify(importJobOperatorValidator).requireOperatorInTenant(1L, 101L);
        verify(importStoredFileCleanupSupport).registerRollbackCleanup("1/key.csv");
        ArgumentCaptor<ImportJobRecord> createdAuditCaptor = ArgumentCaptor.forClass(ImportJobRecord.class);
        verify(importJobAuditService).recordImportJobCreatedEvent(createdAuditCaptor.capture(), eq(101L), eq("req-1"), eq(null));
        assertThat(createdAuditCaptor.getValue().id()).isEqualTo(7001L);
        verify(applicationEventPublisher).publishEvent(new ImportJobCreatedEvent(7001L, 1L));
    }

    @Test
    void createJobShouldPropagateDomainValidationFailure() {
        when(importFileStorageService.store(any(), any(MockMultipartFile.class))).thenReturn("1/key.csv");
        when(importJobCommandUseCase.createQueuedJob(eq(1L), eq(101L), eq("req-1"), any(NewImportJobDraft.class)))
                .thenThrow(new BizException(ErrorCode.BAD_REQUEST, "importType must be USER_CSV"));

        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("ticket_csv");
        MockMultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv", "a".getBytes());

        assertThatThrownBy(() -> importJobSubmissionService.createJob(1L, 101L, "req-1", request, file))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));

        verify(importStoredFileCleanupSupport).registerRollbackCleanup("1/key.csv");
        verifyNoInteractions(importJobAuditService, applicationEventPublisher, importJobQueryService);
    }

    @Test
    void replayFailedRowsShouldCreateDerivedQueuedJobWithSourceLineage() {
        ImportJobRecord sourceJob = sourceJobRecord("users.csv", "1/original.csv");
        ImportJobErrorRecord sourceError = new ImportJobErrorRecord(701L, 1L, 7001L, 3, "UNKNOWN_ROLE", "role missing", "row-3", LocalDateTime.now());
        when(importReplaySourceLoader.loadFailedRowReplay(1L, 7001L))
                .thenReturn(new ImportReplaySourceLoader.ReplayableFailedRows(sourceJob, List.of(sourceError)));
        when(importReplayFileWriter.writeFailedRowReplay(1L, sourceJob, List.of(sourceError)))
                .thenReturn(new ImportReplayFileWriter.ReplayFileBuildResult(sourceJob, "replay-failures-job-7001.csv", "1/replay.csv", 1));
        when(importJobCommandUseCase.createReplayJob(eq(1L), eq(101L), eq("req-replay-1"), any(NewImportJobDraft.class)))
                .thenReturn(jobRecord(7002L, 7001L, "replay-failures-job-7001.csv", "1/replay.csv"));
        when(importJobQueryService.getJobDetail(1L, 7002L))
                .thenReturn(detailResponse(7002L, 7001L, "replay-failures-job-7001.csv", "1/replay.csv", "req-replay-1"));

        ImportJobDetailResponse response = importJobReplayService.replayFailedRows(1L, 101L, "req-replay-1", 7001L);

        assertThat(response.id()).isEqualTo(7002L);
        assertThat(response.sourceJobId()).isEqualTo(7001L);
        ArgumentCaptor<NewImportJobDraft> draftCaptor = ArgumentCaptor.forClass(NewImportJobDraft.class);
        verify(importJobCommandUseCase).createReplayJob(eq(1L), eq(101L), eq("req-replay-1"), draftCaptor.capture());
        assertThat(draftCaptor.getValue().sourceJobId()).isEqualTo(7001L);
        assertThat(draftCaptor.getValue().sourceFilename()).isEqualTo("replay-failures-job-7001.csv");
        verify(importStoredFileCleanupSupport).registerRollbackCleanup("1/replay.csv");
        verify(importJobAuditService).recordReplayRequestedEvent(1L, 101L, "req-replay-1", 7001L, 7002L, 1, Map.of());
        ArgumentCaptor<ImportJobRecord> replayCreatedCaptor = ArgumentCaptor.forClass(ImportJobRecord.class);
        verify(importJobAuditService).recordImportJobCreatedEvent(replayCreatedCaptor.capture(), eq(101L), eq("req-replay-1"), eq(Map.of()));
        assertThat(replayCreatedCaptor.getValue().id()).isEqualTo(7002L);
        verify(applicationEventPublisher).publishEvent(new ImportJobCreatedEvent(7002L, 1L));
    }

    @Test
    void replayWholeFileShouldCreateDerivedQueuedJobWithWholeFileAuditMetadata() {
        ImportJobRecord sourceJob = sourceJobRecord("users.csv", "1/original.csv");
        when(importReplaySourceLoader.loadWholeFileReplay(1L, 7001L))
                .thenReturn(new ImportReplaySourceLoader.WholeFileReplaySource(sourceJob, 3));
        when(importReplayFileWriter.copyWholeFileReplay(1L, sourceJob, 3))
                .thenReturn(new ImportReplayFileWriter.ReplayFileBuildResult(sourceJob, "replay-file-job-7001.csv", "1/replay-file.csv", 3));
        when(importJobCommandUseCase.createReplayJob(eq(1L), eq(101L), eq("req-replay-file-1"), any(NewImportJobDraft.class)))
                .thenReturn(jobRecord(7005L, 7001L, "replay-file-job-7001.csv", "1/replay-file.csv"));
        when(importJobQueryService.getJobDetail(1L, 7005L))
                .thenReturn(detailResponse(7005L, 7001L, "replay-file-job-7001.csv", "1/replay-file.csv", "req-replay-file-1"));

        ImportJobDetailResponse response = importJobReplayService.replayWholeFile(1L, 101L, "req-replay-file-1", 7001L);

        assertThat(response.id()).isEqualTo(7005L);
        verify(importJobAuditService).recordReplayRequestedEvent(1L, 101L, "req-replay-file-1", 7001L, 7005L, 3, Map.of("replayMode", "WHOLE_FILE"));
        ArgumentCaptor<ImportJobRecord> wholeFileCreatedCaptor = ArgumentCaptor.forClass(ImportJobRecord.class);
        verify(importJobAuditService).recordImportJobCreatedEvent(wholeFileCreatedCaptor.capture(), eq(101L), eq("req-replay-file-1"), eq(Map.of("replayMode", "WHOLE_FILE")));
        assertThat(wholeFileCreatedCaptor.getValue().id()).isEqualTo(7005L);
    }

    @Test
    void replayFailedRowsSelectiveShouldNormalizeCodesAndPersistAuditSelection() {
        ImportJobRecord sourceJob = sourceJobRecord("users.csv", "1/original.csv");
        ImportJobErrorRecord sourceError = new ImportJobErrorRecord(701L, 1L, 7001L, 3, "UNKNOWN_ROLE", "role missing", "row-3", LocalDateTime.now());
        when(importReplaySourceLoader.loadSelectiveFailedRowReplay(1L, 7001L, List.of("UNKNOWN_ROLE", "INVALID_EMAIL")))
                .thenReturn(new ImportReplaySourceLoader.ReplayableFailedRows(sourceJob, List.of(sourceError)));
        when(importReplayFileWriter.writeFailedRowReplay(1L, sourceJob, List.of(sourceError)))
                .thenReturn(new ImportReplayFileWriter.ReplayFileBuildResult(sourceJob, "replay-failures-job-7001.csv", "1/replay-selective.csv", 1));
        when(importJobCommandUseCase.createReplayJob(eq(1L), eq(101L), eq("req-replay-selective-1"), any(NewImportJobDraft.class)))
                .thenReturn(jobRecord(7003L, 7001L, "replay-failures-job-7001.csv", "1/replay-selective.csv"));
        when(importJobQueryService.getJobDetail(1L, 7003L))
                .thenReturn(detailResponse(7003L, 7001L, "replay-failures-job-7001.csv", "1/replay-selective.csv", "req-replay-selective-1"));

        ImportJobSelectiveReplayRequest request = new ImportJobSelectiveReplayRequest(List.of(" UNKNOWN_ROLE ", "INVALID_EMAIL", "UNKNOWN_ROLE"));
        ImportJobDetailResponse response = importJobReplayService.replayFailedRowsSelective(1L, 101L, "req-replay-selective-1", 7001L, request);

        assertThat(response.id()).isEqualTo(7003L);
        verify(importReplaySourceLoader).loadSelectiveFailedRowReplay(1L, 7001L, List.of("UNKNOWN_ROLE", "INVALID_EMAIL"));
        verify(importJobAuditService).recordReplayRequestedEvent(1L, 101L, "req-replay-selective-1", 7001L, 7003L, 1, Map.of("selectedErrorCodes", List.of("UNKNOWN_ROLE", "INVALID_EMAIL")));
    }

    @Test
    void replayFailedRowsSelectiveShouldRejectEmptyErrorCodes() {
        ImportJobSelectiveReplayRequest request = new ImportJobSelectiveReplayRequest(List.of());

        assertThatThrownBy(() -> importJobReplayService.replayFailedRowsSelective(1L, 101L, "req-replay-selective-invalid", 7001L, request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));

        verifyNoInteractions(importReplaySourceLoader, importReplayFileWriter);
    }

    @Test
    void replayFailedRowsEditedShouldNormalizeRowsAndPersistScopeOnlyAuditMetadata() {
        ImportJobRecord sourceJob = sourceJobRecord("users.csv", "1/original.csv");
        ImportJobErrorRecord sourceError = new ImportJobErrorRecord(701L, 1L, 7001L, 3, "UNKNOWN_ROLE", "role missing", "row-3", LocalDateTime.now());
        ImportReplayFileWriter.EditedReplayRowReplacement normalizedRow = new ImportReplayFileWriter.EditedReplayRowReplacement(
                701L,
                "retry-user",
                "Retry User",
                "retry-user@demo-shop.local",
                " 123456 ",
                List.of("READ_ONLY", "TENANT_ADMIN")
        );
        when(importReplaySourceLoader.loadEditedReplay(eq(1L), eq(7001L), any()))
                .thenReturn(new ImportReplaySourceLoader.EditedReplaySource(sourceJob, List.of(sourceError), Map.of(701L, normalizedRow)));
        when(importReplayFileWriter.writeEditedReplay(1L, sourceJob, List.of(sourceError), Map.of(701L, normalizedRow)))
                .thenReturn(new ImportReplayFileWriter.ReplayFileBuildResult(sourceJob, "replay-edited-job-7001.csv", "1/replay-edited.csv", 1));
        when(importJobCommandUseCase.createReplayJob(eq(1L), eq(101L), eq("req-replay-edited-1"), any(NewImportJobDraft.class)))
                .thenReturn(jobRecord(7004L, 7001L, "replay-edited-job-7001.csv", "1/replay-edited.csv"));
        when(importJobQueryService.getJobDetail(1L, 7004L))
                .thenReturn(detailResponse(7004L, 7001L, "replay-edited-job-7001.csv", "1/replay-edited.csv", "req-replay-edited-1"));

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

        ImportJobDetailResponse response = importJobReplayService.replayFailedRowsEdited(1L, 101L, "req-replay-edited-1", 7001L, request);

        assertThat(response.id()).isEqualTo(7004L);
        ArgumentCaptor<List<ImportReplayFileWriter.EditedReplayRowReplacement>> rowsCaptor = editedReplayRowsCaptor();
        verify(importReplaySourceLoader).loadEditedReplay(eq(1L), eq(7001L), rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).containsExactly(normalizedRow);
        verify(importJobAuditService).recordReplayRequestedEvent(
                1L,
                101L,
                "req-replay-edited-1",
                7001L,
                7004L,
                1,
                Map.of("editedErrorIds", List.of(701L), "editedRowCount", 1, "editedFields", List.of("username", "displayName", "email", "password", "roleCodes"))
        );
    }

    @Test
    void replayFailedRowsEditedShouldRejectDuplicateErrorIds() {
        ImportJobEditedReplayRequest request = new ImportJobEditedReplayRequest(List.of(
                new ImportJobEditedReplayItemRequest(701L, "retry-a", "Retry A", "retry-a@demo-shop.local", "123456", List.of("READ_ONLY")),
                new ImportJobEditedReplayItemRequest(701L, "retry-b", "Retry B", "retry-b@demo-shop.local", "123456", List.of("READ_ONLY"))
        ));

        assertThatThrownBy(() -> importJobReplayService.replayFailedRowsEdited(1L, 101L, "req-replay-edited-dup", 7001L, request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(biz.getMessage()).contains("duplicate errorId");
                });

        verifyNoInteractions(importReplaySourceLoader, importReplayFileWriter);
    }

    private ImportJobRecord sourceJobRecord(String sourceFilename, String storageKey) {
        return new ImportJobRecord(
                7001L, 1L, "USER_CSV", "CSV", sourceFilename, storageKey, null,
                "FAILED", 101L, "req-source", 1, 0, 1, "all rows failed validation", LocalDateTime.now(), null, null
        );
    }

    private ImportJobRecord jobRecord(Long id, Long sourceJobId, String sourceFilename, String storageKey) {
        return new ImportJobRecord(
                id, 1L, "USER_CSV", "CSV", sourceFilename, storageKey, sourceJobId,
                "QUEUED", 101L, "req-1", 0, 0, 0, null, LocalDateTime.now(), null, null
        );
    }

    private ImportJobDetailResponse detailResponse(Long id,
                                                   Long sourceJobId,
                                                   String sourceFilename,
                                                   String storageKey,
                                                   String requestId) {
        return new ImportJobDetailResponse(
                id, 1L, "USER_CSV", "CSV", sourceFilename, storageKey, sourceJobId, "QUEUED", 101L, requestId,
                0, 0, 0, null, null, null, null, List.of(), List.of()
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ArgumentCaptor<List<ImportReplayFileWriter.EditedReplayRowReplacement>> editedReplayRowsCaptor() {
        return ArgumentCaptor.forClass((Class) List.class);
    }
}
