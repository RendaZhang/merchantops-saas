package com.renda.merchantops.api.service;

import com.renda.merchantops.api.context.RequestIdPolicy;
import com.renda.merchantops.api.dto.importjob.command.ImportJobCreateRequest;
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
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportJobCommandService {

    private static final String IMPORT_SOURCE_TYPE_CSV = "CSV";

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

        recordImportJobCreatedEvent(saved, operatorId, resolvedRequestId);

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
        replayJob.setRequestId(resolvedRequestId);
        replayJob.setTotalCount(0);
        replayJob.setSuccessCount(0);
        replayJob.setFailureCount(0);
        replayJob.setCreatedAt(LocalDateTime.now());
        ImportJobEntity savedReplayJob = importJobRepository.save(replayJob);

        auditEventService.recordEvent(
                tenantId,
                "IMPORT_JOB",
                sourceJob.getId(),
                "IMPORT_JOB_REPLAY_REQUESTED",
                operatorId,
                resolvedRequestId,
                null,
                Map.of(
                        "replayJobId", savedReplayJob.getId(),
                        "replayedFailureCount", replayFile.replayRowCount()
                )
        );
        recordImportJobCreatedEvent(savedReplayJob, operatorId, resolvedRequestId);

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

    private void recordImportJobCreatedEvent(ImportJobEntity job, Long operatorId, String requestId) {
        Map<String, Object> afterValue = new java.util.LinkedHashMap<>();
        afterValue.put("status", job.getStatus());
        afterValue.put("importType", job.getImportType());
        afterValue.put("sourceFilename", job.getSourceFilename());
        if (job.getSourceJobId() != null) {
            afterValue.put("sourceJobId", job.getSourceJobId());
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
