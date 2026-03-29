package com.renda.merchantops.infra.importjob;

import com.renda.merchantops.domain.importjob.ImportJobAiInteractionItem;
import com.renda.merchantops.domain.importjob.ImportJobAiInteractionPageCriteria;
import com.renda.merchantops.domain.importjob.ImportJobAiInteractionPageResult;
import com.renda.merchantops.domain.importjob.ImportJobCommandPort;
import com.renda.merchantops.domain.importjob.ImportJobErrorCount;
import com.renda.merchantops.domain.importjob.ImportJobErrorPageCriteria;
import com.renda.merchantops.domain.importjob.ImportJobErrorPageResult;
import com.renda.merchantops.domain.importjob.ImportJobErrorRecord;
import com.renda.merchantops.domain.importjob.ImportJobPageCriteria;
import com.renda.merchantops.domain.importjob.ImportJobPageResult;
import com.renda.merchantops.domain.importjob.ImportJobQueryPort;
import com.renda.merchantops.domain.importjob.ImportJobRecord;
import com.renda.merchantops.infra.persistence.entity.AiInteractionRecordEntity;
import com.renda.merchantops.infra.persistence.entity.ImportJobEntity;
import com.renda.merchantops.infra.persistence.entity.ImportJobItemErrorEntity;
import com.renda.merchantops.infra.repository.AiInteractionRecordRepository;
import com.renda.merchantops.infra.repository.ImportJobItemErrorRepository;
import com.renda.merchantops.infra.repository.ImportJobRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class JpaImportJobAdapter implements ImportJobQueryPort, ImportJobCommandPort {

    private static final String ENTITY_TYPE_IMPORT_JOB = "IMPORT_JOB";

    private final ImportJobRepository importJobRepository;
    private final ImportJobItemErrorRepository importJobItemErrorRepository;
    private final AiInteractionRecordRepository aiInteractionRecordRepository;

    public JpaImportJobAdapter(ImportJobRepository importJobRepository,
                               ImportJobItemErrorRepository importJobItemErrorRepository,
                               AiInteractionRecordRepository aiInteractionRecordRepository) {
        this.importJobRepository = importJobRepository;
        this.importJobItemErrorRepository = importJobItemErrorRepository;
        this.aiInteractionRecordRepository = aiInteractionRecordRepository;
    }

    @Override
    public ImportJobPageResult pageJobs(Long tenantId, ImportJobPageCriteria criteria) {
        PageRequest pageable = PageRequest.of(
                criteria.page(),
                criteria.size(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        var page = importJobRepository.searchPageByTenantId(
                tenantId,
                criteria.status(),
                criteria.importType(),
                criteria.requestedBy(),
                criteria.hasFailuresOnly(),
                pageable
        );
        return new ImportJobPageResult(
                page.getContent().stream().map(this::toImportJobRecord).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    public Optional<ImportJobRecord> findJob(Long tenantId, Long importJobId) {
        return importJobRepository.findByIdAndTenantId(importJobId, tenantId).map(this::toImportJobRecord);
    }

    @Override
    public ImportJobAiInteractionPageResult pageJobAiInteractions(Long tenantId,
                                                                  Long importJobId,
                                                                  ImportJobAiInteractionPageCriteria criteria) {
        var page = aiInteractionRecordRepository.searchPageByTenantIdAndEntity(
                tenantId,
                ENTITY_TYPE_IMPORT_JOB,
                importJobId,
                criteria.interactionType(),
                criteria.status(),
                PageRequest.of(
                        criteria.page(),
                        criteria.size(),
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
                )
        );
        return new ImportJobAiInteractionPageResult(
                page.getContent().stream().map(this::toImportJobAiInteractionItem).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    public Optional<ImportJobAiInteractionItem> findJobAiInteraction(Long tenantId, Long importJobId, Long interactionId) {
        return aiInteractionRecordRepository.findByIdAndTenantIdAndEntityTypeAndEntityId(
                        interactionId,
                        tenantId,
                        ENTITY_TYPE_IMPORT_JOB,
                        importJobId
                )
                .map(this::toImportJobAiInteractionItem);
    }

    @Override
    public ImportJobErrorPageResult pageJobErrors(Long tenantId,
                                                  Long importJobId,
                                                  ImportJobErrorPageCriteria criteria) {
        var page = importJobItemErrorRepository.searchPageByTenantIdAndImportJobId(
                tenantId,
                importJobId,
                criteria.errorCode(),
                PageRequest.of(criteria.page(), criteria.size())
        );
        return new ImportJobErrorPageResult(
                page.getContent().stream().map(this::toImportJobErrorRecord).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    public List<ImportJobErrorCount> summarizeErrorCodes(Long tenantId, Long importJobId) {
        return importJobItemErrorRepository.summarizeErrorCodesByTenantIdAndImportJobId(tenantId, importJobId)
                .stream()
                .map(summary -> new ImportJobErrorCount(summary.getErrorCode(), summary.getErrorCount()))
                .toList();
    }

    @Override
    public List<ImportJobErrorRecord> listJobErrors(Long tenantId, Long importJobId) {
        return importJobItemErrorRepository.findAllByTenantIdAndImportJobIdOrderByRowNumberAscIdAsc(tenantId, importJobId)
                .stream()
                .map(this::toImportJobErrorRecord)
                .toList();
    }

    @Override
    public List<ImportJobRecord> findQueuedJobsForRecovery(LocalDateTime createdBefore, int limit) {
        return importJobRepository.findQueuedJobsForEnqueueRecovery(
                        "QUEUED",
                        createdBefore,
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(this::toImportJobRecord)
                .toList();
    }

    @Override
    public List<ImportJobRecord> findStaleProcessingJobsForRecovery(LocalDateTime startedBefore, int limit) {
        return importJobRepository.findStaleProcessingJobsForRecovery(
                        "PROCESSING",
                        startedBefore,
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(this::toImportJobRecord)
                .toList();
    }

    @Override
    public ImportJobRecord saveJob(ImportJobRecord job) {
        ImportJobEntity entity = new ImportJobEntity();
        entity.setId(job.id());
        entity.setTenantId(job.tenantId());
        entity.setImportType(job.importType());
        entity.setSourceType(job.sourceType());
        entity.setSourceFilename(job.sourceFilename());
        entity.setStorageKey(job.storageKey());
        entity.setSourceJobId(job.sourceJobId());
        entity.setStatus(job.status());
        entity.setRequestedBy(job.requestedBy());
        entity.setRequestId(job.requestId());
        entity.setTotalCount(job.totalCount());
        entity.setSuccessCount(job.successCount());
        entity.setFailureCount(job.failureCount());
        entity.setErrorSummary(job.errorSummary());
        entity.setCreatedAt(job.createdAt());
        entity.setStartedAt(job.startedAt());
        entity.setFinishedAt(job.finishedAt());
        return toImportJobRecord(importJobRepository.save(entity));
    }

    @Override
    public Optional<ImportJobRecord> findJobForUpdate(Long tenantId, Long importJobId) {
        return importJobRepository.findByIdAndTenantIdForUpdate(importJobId, tenantId).map(this::toImportJobRecord);
    }

    @Override
    public void saveJobError(ImportJobErrorRecord error) {
        ImportJobItemErrorEntity entity = new ImportJobItemErrorEntity();
        entity.setId(error.id());
        entity.setTenantId(error.tenantId());
        entity.setImportJobId(error.importJobId());
        entity.setRowNumber(error.rowNumber());
        entity.setErrorCode(error.errorCode());
        entity.setErrorMessage(error.errorMessage());
        entity.setRawPayload(error.rawPayload());
        entity.setCreatedAt(error.createdAt());
        importJobItemErrorRepository.save(entity);
    }

    private ImportJobRecord toImportJobRecord(ImportJobEntity entity) {
        return new ImportJobRecord(
                entity.getId(),
                entity.getTenantId(),
                entity.getImportType(),
                entity.getSourceType(),
                entity.getSourceFilename(),
                entity.getStorageKey(),
                entity.getSourceJobId(),
                entity.getStatus(),
                entity.getRequestedBy(),
                entity.getRequestId(),
                entity.getTotalCount(),
                entity.getSuccessCount(),
                entity.getFailureCount(),
                entity.getErrorSummary(),
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getFinishedAt()
        );
    }

    private ImportJobErrorRecord toImportJobErrorRecord(ImportJobItemErrorEntity entity) {
        return new ImportJobErrorRecord(
                entity.getId(),
                entity.getTenantId(),
                entity.getImportJobId(),
                entity.getRowNumber(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getRawPayload(),
                entity.getCreatedAt()
        );
    }

    private ImportJobAiInteractionItem toImportJobAiInteractionItem(AiInteractionRecordEntity entity) {
        return new ImportJobAiInteractionItem(
                entity.getId(),
                entity.getInteractionType(),
                entity.getStatus(),
                entity.getOutputSummary(),
                entity.getPromptVersion(),
                entity.getModelId(),
                entity.getLatencyMs(),
                entity.getRequestId(),
                entity.getUsagePromptTokens(),
                entity.getUsageCompletionTokens(),
                entity.getUsageTotalTokens(),
                entity.getUsageCostMicros(),
                entity.getCreatedAt()
        );
    }
}
