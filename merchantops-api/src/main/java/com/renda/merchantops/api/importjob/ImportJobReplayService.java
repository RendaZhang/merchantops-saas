package com.renda.merchantops.api.importjob;

import com.renda.merchantops.api.context.RequestIdPolicy;
import com.renda.merchantops.api.dto.importjob.command.ImportJobEditedReplayItemRequest;
import com.renda.merchantops.api.dto.importjob.command.ImportJobEditedReplayRequest;
import com.renda.merchantops.api.dto.importjob.command.ImportJobSelectiveReplayRequest;
import com.renda.merchantops.api.dto.importjob.query.ImportJobDetailResponse;
import com.renda.merchantops.api.importjob.messaging.ImportJobCreatedEvent;
import com.renda.merchantops.api.importjob.replay.ImportReplayFileWriter;
import com.renda.merchantops.api.importjob.replay.ImportReplaySourceLoader;
import com.renda.merchantops.domain.importjob.ImportJobCommandUseCase;
import com.renda.merchantops.domain.importjob.ImportJobRecord;
import com.renda.merchantops.domain.importjob.NewImportJobDraft;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImportJobReplayService {

    private static final String REPLAY_MODE_WHOLE_FILE = "WHOLE_FILE";
    private static final List<String> EDITED_REPLAY_AUDIT_FIELDS = List.of(
            "username",
            "displayName",
            "email",
            "password",
            "roleCodes"
    );

    private final ImportJobCommandUseCase importJobCommandUseCase;
    private final ImportJobOperatorValidator importJobOperatorValidator;
    private final ImportStoredFileCleanupSupport importStoredFileCleanupSupport;
    private final ImportJobQueryService importJobQueryService;
    private final ImportJobAuditService importJobAuditService;
    private final ImportReplaySourceLoader importReplaySourceLoader;
    private final ImportReplayFileWriter importReplayFileWriter;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public ImportJobDetailResponse replayFailedRows(Long tenantId,
                                                    Long operatorId,
                                                    String requestId,
                                                    Long sourceJobId) {
        String resolvedRequestId = RequestIdPolicy.requireNormalized(requestId);
        importJobOperatorValidator.requireOperatorInTenant(tenantId, operatorId);
        // Failed-row replay keeps the original raw rows untouched and only narrows the source
        // set to rows that previously recorded import errors.
        var replaySource = importReplaySourceLoader.loadFailedRowReplay(tenantId, sourceJobId);
        ImportReplayFileWriter.ReplayFileBuildResult replayFile = importReplayFileWriter.writeFailedRowReplay(
                tenantId,
                replaySource.sourceJob(),
                replaySource.failedRows()
        );
        return createReplayJob(tenantId, operatorId, resolvedRequestId, replayFile, Map.of());
    }

    @Transactional
    public ImportJobDetailResponse replayWholeFile(Long tenantId,
                                                   Long operatorId,
                                                   String requestId,
                                                   Long sourceJobId) {
        String resolvedRequestId = RequestIdPolicy.requireNormalized(requestId);
        importJobOperatorValidator.requireOperatorInTenant(tenantId, operatorId);
        // Whole-file replay intentionally clones the original source so downstream audit can
        // distinguish a full rerun from narrower failed-row retry modes.
        var replaySource = importReplaySourceLoader.loadWholeFileReplay(tenantId, sourceJobId);
        ImportReplayFileWriter.ReplayFileBuildResult replayFile = importReplayFileWriter.copyWholeFileReplay(
                tenantId,
                replaySource.sourceJob(),
                replaySource.replayRowCount()
        );
        return createReplayJob(
                tenantId,
                operatorId,
                resolvedRequestId,
                replayFile,
                buildReplayModeAuditMetadata(REPLAY_MODE_WHOLE_FILE)
        );
    }

    @Transactional
    public ImportJobDetailResponse replayFailedRowsSelective(Long tenantId,
                                                             Long operatorId,
                                                             String requestId,
                                                             Long sourceJobId,
                                                             ImportJobSelectiveReplayRequest request) {
        String resolvedRequestId = RequestIdPolicy.requireNormalized(requestId);
        importJobOperatorValidator.requireOperatorInTenant(tenantId, operatorId);
        List<String> selectedErrorCodes = normalizeSelectedErrorCodes(request == null ? null : request.getErrorCodes());
        var replaySource = importReplaySourceLoader.loadSelectiveFailedRowReplay(tenantId, sourceJobId, selectedErrorCodes);
        ImportReplayFileWriter.ReplayFileBuildResult replayFile = importReplayFileWriter.writeFailedRowReplay(
                tenantId,
                replaySource.sourceJob(),
                replaySource.failedRows()
        );
        return createReplayJob(
                tenantId,
                operatorId,
                resolvedRequestId,
                replayFile,
                buildSelectiveReplayAuditMetadata(selectedErrorCodes)
        );
    }

    @Transactional
    public ImportJobDetailResponse replayFailedRowsEdited(Long tenantId,
                                                          Long operatorId,
                                                          String requestId,
                                                          Long sourceJobId,
                                                          ImportJobEditedReplayRequest request) {
        String resolvedRequestId = RequestIdPolicy.requireNormalized(requestId);
        importJobOperatorValidator.requireOperatorInTenant(tenantId, operatorId);
        // Edited replay is the only path that mutates row payload before execution, so input
        // normalization and duplicate-errorId protection happen before any file is written.
        List<ImportReplayFileWriter.EditedReplayRowReplacement> editedRows = normalizeEditedReplayItems(
                request == null ? null : request.getItems()
        );
        var replaySource = importReplaySourceLoader.loadEditedReplay(tenantId, sourceJobId, editedRows);
        ImportReplayFileWriter.ReplayFileBuildResult replayFile = importReplayFileWriter.writeEditedReplay(
                tenantId,
                replaySource.sourceJob(),
                replaySource.sourceErrors(),
                replaySource.editedRowsByErrorId()
        );
        return createReplayJob(
                tenantId,
                operatorId,
                resolvedRequestId,
                replayFile,
                buildEditedReplayAuditMetadata(editedRows)
        );
    }

    private ImportJobDetailResponse createReplayJob(Long tenantId,
                                                    Long operatorId,
                                                    String requestId,
                                                    ImportReplayFileWriter.ReplayFileBuildResult replayFile,
                                                    Map<String, Object> replayMetadata) {
        // Register cleanup before saving the derived job so transaction rollback removes any
        // staged replay file that would otherwise become an orphan in storage.
        importStoredFileCleanupSupport.registerRollbackCleanup(replayFile.storageKey());

        ImportJobRecord sourceJob = replayFile.sourceJob();
        ImportJobRecord savedReplayJob = importJobCommandUseCase.createReplayJob(
                tenantId,
                operatorId,
                requestId,
                new NewImportJobDraft(
                        sourceJob.importType(),
                        sourceJob.sourceType(),
                        replayFile.sourceFilename(),
                        replayFile.storageKey(),
                        sourceJob.id()
                )
        );

        importJobAuditService.recordReplayRequestedEvent(
                tenantId,
                operatorId,
                requestId,
                sourceJob.id(),
                savedReplayJob.id(),
                replayFile.replayRowCount(),
                replayMetadata
        );
        importJobAuditService.recordImportJobCreatedEvent(savedReplayJob, operatorId, requestId, replayMetadata);

        applicationEventPublisher.publishEvent(new ImportJobCreatedEvent(savedReplayJob.id(), tenantId));
        return importJobQueryService.getJobDetail(tenantId, savedReplayJob.id());
    }

    private List<String> normalizeSelectedErrorCodes(List<String> errorCodes) {
        if (errorCodes == null || errorCodes.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "errorCodes must not be empty");
        }
        List<String> normalized = errorCodes.stream()
                .map(errorCode -> {
                    if (!StringUtils.hasText(errorCode)) {
                        throw new BizException(ErrorCode.BAD_REQUEST, "errorCodes must not contain blank values");
                    }
                    return errorCode.trim();
                })
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "errorCodes must not be empty");
        }
        return normalized;
    }

    private List<ImportReplayFileWriter.EditedReplayRowReplacement> normalizeEditedReplayItems(List<ImportJobEditedReplayItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "items must not be empty");
        }
        List<ImportReplayFileWriter.EditedReplayRowReplacement> normalized = new ArrayList<>();
        Set<Long> seenErrorIds = new HashSet<>();
        for (ImportJobEditedReplayItemRequest item : items) {
            if (item == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "items must not contain null entries");
            }
            if (item.getErrorId() == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "errorId must not be null");
            }
            if (!seenErrorIds.add(item.getErrorId())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "items must not contain duplicate errorId");
            }
            normalized.add(new ImportReplayFileWriter.EditedReplayRowReplacement(
                    item.getErrorId(),
                    requireNonBlank(item.getUsername(), "username"),
                    requireNonBlank(item.getDisplayName(), "displayName"),
                    requireNonBlank(item.getEmail(), "email"),
                    requireNonBlankPreserve(item.getPassword(), "password"),
                    normalizeRoleCodes(item.getRoleCodes())
            ));
        }
        return List.copyOf(normalized);
    }

    private List<String> normalizeRoleCodes(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "roleCodes must not be empty");
        }
        List<String> normalized = new ArrayList<>();
        for (String roleCode : roleCodes) {
            if (!StringUtils.hasText(roleCode)) {
                throw new BizException(ErrorCode.BAD_REQUEST, "roleCodes must not contain blank values");
            }
            String trimmed = roleCode.trim();
            if (!normalized.contains(trimmed)) {
                normalized.add(trimmed);
            }
        }
        if (normalized.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "roleCodes must not be empty");
        }
        return normalized;
    }

    private Map<String, Object> buildSelectiveReplayAuditMetadata(List<String> selectedErrorCodes) {
        if (selectedErrorCodes == null || selectedErrorCodes.isEmpty()) {
            return Map.of();
        }
        return Map.of("selectedErrorCodes", selectedErrorCodes);
    }

    private Map<String, Object> buildEditedReplayAuditMetadata(List<ImportReplayFileWriter.EditedReplayRowReplacement> editedRows) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("editedErrorIds", editedRows.stream().map(ImportReplayFileWriter.EditedReplayRowReplacement::errorId).toList());
        metadata.put("editedRowCount", editedRows.size());
        metadata.put("editedFields", EDITED_REPLAY_AUDIT_FIELDS);
        return metadata;
    }

    private Map<String, Object> buildReplayModeAuditMetadata(String replayMode) {
        return Map.of("replayMode", replayMode);
    }

    private String requireNonBlank(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " must not be blank");
        }
        return value.trim();
    }

    private String requireNonBlankPreserve(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " must not be blank");
        }
        return value;
    }
}
