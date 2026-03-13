package com.renda.merchantops.api.service;

import com.renda.merchantops.api.context.RequestIdPolicy;
import com.renda.merchantops.api.dto.importjob.command.ImportJobCreateRequest;
import com.renda.merchantops.api.dto.importjob.command.ImportJobEditedReplayItemRequest;
import com.renda.merchantops.api.dto.importjob.command.ImportJobEditedReplayRequest;
import com.renda.merchantops.api.dto.importjob.command.ImportJobSelectiveReplayRequest;
import com.renda.merchantops.api.dto.importjob.query.ImportJobDetailResponse;
import com.renda.merchantops.api.messaging.ImportJobCreatedEvent;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.ImportJobEntity;
import com.renda.merchantops.infra.repository.ImportJobRepository;
import com.renda.merchantops.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportJobCommandService {

    private static final String IMPORT_SOURCE_TYPE_CSV = "CSV";
    private static final String REPLAY_MODE_WHOLE_FILE = "WHOLE_FILE";
    private static final List<String> EDITED_REPLAY_AUDIT_FIELDS = List.of(
            "username",
            "displayName",
            "email",
            "password",
            "roleCodes"
    );

    private final ImportJobRepository importJobRepository;
    private final ImportFileStorageService importFileStorageService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ImportJobQueryService importJobQueryService;
    private final ImportReplayFileBuilder importReplayFileBuilder;
    private final AuditEventService auditEventService;
    private final UserRepository userRepository;

    @Transactional
    public ImportJobDetailResponse createJob(Long tenantId,
                                             Long operatorId,
                                             String requestId,
                                             ImportJobCreateRequest request,
                                             MultipartFile file) {
        String resolvedRequestId = RequestIdPolicy.requireNormalized(requestId);
        requireOperatorInTenant(tenantId, operatorId);
        String importType = normalizeImportType(request == null ? null : request.getImportType());
        String storageKey = importFileStorageService.store(tenantId, file);
        registerRollbackCleanup(storageKey);

        ImportJobEntity entity = new ImportJobEntity();
        entity.setTenantId(tenantId);
        entity.setImportType(importType);
        entity.setSourceType(IMPORT_SOURCE_TYPE_CSV);
        entity.setSourceFilename(resolveFilename(file));
        entity.setStorageKey(storageKey);
        entity.setSourceJobId(null);
        entity.setStatus("QUEUED");
        entity.setRequestedBy(operatorId);
        entity.setRequestId(resolvedRequestId);
        entity.setTotalCount(0);
        entity.setSuccessCount(0);
        entity.setFailureCount(0);
        entity.setCreatedAt(LocalDateTime.now());
        ImportJobEntity saved = importJobRepository.save(entity);

        recordImportJobCreatedEvent(saved, operatorId, resolvedRequestId, null);

        applicationEventPublisher.publishEvent(new ImportJobCreatedEvent(saved.getId(), tenantId));

        return importJobQueryService.toDetail(saved);
    }

    @Transactional
    public ImportJobDetailResponse replayFailedRows(Long tenantId,
                                                    Long operatorId,
                                                    String requestId,
                                                    Long sourceJobId) {
        String resolvedRequestId = RequestIdPolicy.requireNormalized(requestId);
        requireOperatorInTenant(tenantId, operatorId);

        ImportReplayFileBuilder.ReplayFileBuildResult replayFile = importReplayFileBuilder
                .buildFailedRowReplay(tenantId, sourceJobId);
        return createReplayJob(tenantId, operatorId, resolvedRequestId, replayFile, Map.of());
    }

    @Transactional
    public ImportJobDetailResponse replayWholeFile(Long tenantId,
                                                   Long operatorId,
                                                   String requestId,
                                                   Long sourceJobId) {
        String resolvedRequestId = RequestIdPolicy.requireNormalized(requestId);
        requireOperatorInTenant(tenantId, operatorId);

        ImportReplayFileBuilder.ReplayFileBuildResult replayFile = importReplayFileBuilder
                .buildWholeFileReplay(tenantId, sourceJobId);
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
        requireOperatorInTenant(tenantId, operatorId);
        List<String> selectedErrorCodes = normalizeSelectedErrorCodes(request == null ? null : request.getErrorCodes());

        ImportReplayFileBuilder.ReplayFileBuildResult replayFile = importReplayFileBuilder
                .buildSelectiveFailedRowReplay(tenantId, sourceJobId, selectedErrorCodes);
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
        requireOperatorInTenant(tenantId, operatorId);
        List<ImportReplayFileBuilder.EditedReplayRowReplacement> editedRows = normalizeEditedReplayItems(
                request == null ? null : request.getItems()
        );

        ImportReplayFileBuilder.ReplayFileBuildResult replayFile = importReplayFileBuilder
                .buildEditedFailedRowReplay(tenantId, sourceJobId, editedRows);
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
                                                    ImportReplayFileBuilder.ReplayFileBuildResult replayFile,
                                                    Map<String, Object> replayMetadata) {
        registerRollbackCleanup(replayFile.storageKey());

        ImportJobEntity sourceJob = replayFile.sourceJob();
        ImportJobEntity replayJob = new ImportJobEntity();
        replayJob.setTenantId(tenantId);
        replayJob.setImportType(sourceJob.getImportType());
        replayJob.setSourceType(sourceJob.getSourceType());
        replayJob.setSourceFilename(replayFile.sourceFilename());
        replayJob.setStorageKey(replayFile.storageKey());
        replayJob.setSourceJobId(sourceJob.getId());
        replayJob.setStatus("QUEUED");
        replayJob.setRequestedBy(operatorId);
        replayJob.setRequestId(requestId);
        replayJob.setTotalCount(0);
        replayJob.setSuccessCount(0);
        replayJob.setFailureCount(0);
        replayJob.setCreatedAt(LocalDateTime.now());
        ImportJobEntity savedReplayJob = importJobRepository.save(replayJob);

        recordReplayRequestedEvent(
                tenantId,
                operatorId,
                requestId,
                sourceJob.getId(),
                savedReplayJob.getId(),
                replayFile.replayRowCount(),
                replayMetadata
        );
        recordImportJobCreatedEvent(savedReplayJob, operatorId, requestId, replayMetadata);

        applicationEventPublisher.publishEvent(new ImportJobCreatedEvent(savedReplayJob.getId(), tenantId));
        return importJobQueryService.toDetail(savedReplayJob);
    }

    private void requireOperatorInTenant(Long tenantId, Long operatorId) {
        if (tenantId == null || operatorId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "user context missing");
        }
        if (userRepository.findByIdAndTenantId(operatorId, tenantId).isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "operator does not belong to tenant");
        }
    }

    private void recordReplayRequestedEvent(Long tenantId,
                                            Long operatorId,
                                            String requestId,
                                            Long sourceJobId,
                                            Long replayJobId,
                                            int replayRowCount,
                                            Map<String, Object> replayMetadata) {
        Map<String, Object> afterValue = new LinkedHashMap<>();
        afterValue.put("replayJobId", replayJobId);
        afterValue.put("replayedFailureCount", replayRowCount);
        if (replayMetadata != null && !replayMetadata.isEmpty()) {
            afterValue.putAll(replayMetadata);
        }
        auditEventService.recordEvent(
                tenantId,
                "IMPORT_JOB",
                sourceJobId,
                "IMPORT_JOB_REPLAY_REQUESTED",
                operatorId,
                requestId,
                null,
                afterValue
        );
    }

    private void recordImportJobCreatedEvent(ImportJobEntity job,
                                             Long operatorId,
                                             String requestId,
                                             Map<String, Object> replayMetadata) {
        Map<String, Object> afterValue = new LinkedHashMap<>();
        afterValue.put("status", job.getStatus());
        afterValue.put("importType", job.getImportType());
        afterValue.put("sourceFilename", job.getSourceFilename());
        if (job.getSourceJobId() != null) {
            afterValue.put("sourceJobId", job.getSourceJobId());
        }
        if (replayMetadata != null && !replayMetadata.isEmpty()) {
            afterValue.putAll(replayMetadata);
        }
        auditEventService.recordEvent(
                job.getTenantId(),
                "IMPORT_JOB",
                job.getId(),
                "IMPORT_JOB_CREATED",
                operatorId,
                requestId,
                null,
                afterValue
        );
    }

    private String normalizeImportType(String importType) {
        if (!StringUtils.hasText(importType)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "importType must not be blank");
        }
        String normalized = importType.trim().toUpperCase(Locale.ROOT);
        if (!"USER_CSV".equals(normalized)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "importType must be USER_CSV");
        }
        return normalized;
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

    private List<ImportReplayFileBuilder.EditedReplayRowReplacement> normalizeEditedReplayItems(List<ImportJobEditedReplayItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "items must not be empty");
        }
        List<ImportReplayFileBuilder.EditedReplayRowReplacement> normalized = new ArrayList<>();
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
            normalized.add(new ImportReplayFileBuilder.EditedReplayRowReplacement(
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

    private Map<String, Object> buildEditedReplayAuditMetadata(List<ImportReplayFileBuilder.EditedReplayRowReplacement> editedRows) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("editedErrorIds", editedRows.stream()
                .map(ImportReplayFileBuilder.EditedReplayRowReplacement::errorId)
                .toList());
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

    private String resolveFilename(MultipartFile file) {
        String filename = file == null ? null : file.getOriginalFilename();
        return StringUtils.hasText(filename) ? filename.trim() : "upload.csv";
    }

    private void registerRollbackCleanup(String storageKey) {
        if (!StringUtils.hasText(storageKey) || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteStoredFileQuietly(storageKey);
                }
            }
        });
    }

    private void deleteStoredFileQuietly(String storageKey) {
        try {
            importFileStorageService.delete(storageKey);
        } catch (Exception ex) {
            log.warn("failed to delete import file {} after rollback", storageKey, ex);
        }
    }
}
