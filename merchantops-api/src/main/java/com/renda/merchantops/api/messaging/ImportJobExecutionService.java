package com.renda.merchantops.api.messaging;

import com.renda.merchantops.api.config.ImportProcessingProperties;
import com.renda.merchantops.api.service.AuditEventService;
import com.renda.merchantops.api.service.ImportCsvSupport;
import com.renda.merchantops.infra.persistence.entity.ImportJobEntity;
import com.renda.merchantops.infra.persistence.entity.ImportJobItemErrorEntity;
import com.renda.merchantops.infra.repository.ImportJobItemErrorRepository;
import com.renda.merchantops.infra.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImportJobExecutionService {

    private static final String STATUS_QUEUED = "QUEUED";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SUCCEEDED = "SUCCEEDED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String IMPORT_TYPE_USER_CSV = "USER_CSV";
    private static final String PROCESSING_STALE_ERROR = "PROCESSING_STALE";
    private static final String PROCESSING_STALE_ERROR_SUMMARY = "import job processing expired after partial progress";

    private final ImportJobRepository importJobRepository;
    private final ImportJobItemErrorRepository importJobItemErrorRepository;
    private final AuditEventService auditEventService;
    private final UserCsvImportProcessor userCsvImportProcessor;
    private final ImportProcessingProperties importProcessingProperties;

    @Transactional
    public ImportJobStartResult startProcessing(Long jobId, Long tenantId) {
        if (jobId == null || tenantId == null) {
            return ImportJobStartResult.ignore();
        }
        ImportJobEntity job = importJobRepository.findByIdAndTenantIdForUpdate(jobId, tenantId).orElse(null);
        if (job == null) {
            return ImportJobStartResult.ignore();
        }
        if (STATUS_QUEUED.equals(job.getStatus())) {
            return ImportJobStartResult.started(transitionToProcessing(job, false));
        }
        if (STATUS_PROCESSING.equals(job.getStatus())) {
            return recoverOrFailStaleProcessing(job);
        }
        return ImportJobStartResult.ignore();
    }

    private ImportJobStartResult recoverOrFailStaleProcessing(ImportJobEntity job) {
        if (!isStaleProcessing(job)) {
            return ImportJobStartResult.requeue();
        }
        if (hasProgress(job)) {
            failStaleProcessingJob(job);
            return ImportJobStartResult.ignore();
        }
        return ImportJobStartResult.started(transitionToProcessing(job, true));
    }

    private ImportJobExecutionContext transitionToProcessing(ImportJobEntity job, boolean recoveredFromStale) {
        LocalDateTime now = LocalDateTime.now();
        job.setStatus(STATUS_PROCESSING);
        job.setStartedAt(now);
        job.setFinishedAt(null);
        importJobRepository.save(job);
        Map<String, Object> afterValue = new LinkedHashMap<>();
        afterValue.put("status", STATUS_PROCESSING);
        if (recoveredFromStale) {
            afterValue.put("recoveredFromStale", true);
        }
        auditEventService.recordEvent(
                job.getTenantId(),
                "IMPORT_JOB",
                job.getId(),
                "IMPORT_JOB_PROCESSING_STARTED",
                job.getRequestedBy(),
                job.getRequestId(),
                null,
                afterValue
        );
        return toExecutionContext(job);
    }

    private void failStaleProcessingJob(ImportJobEntity job) {
        job.setStatus(STATUS_FAILED);
        job.setFinishedAt(LocalDateTime.now());
        job.setErrorSummary(PROCESSING_STALE_ERROR_SUMMARY);
        importJobRepository.save(job);
        auditEventService.recordEvent(
                job.getTenantId(),
                "IMPORT_JOB",
                job.getId(),
                "IMPORT_JOB_FAILED",
                job.getRequestedBy(),
                job.getRequestId(),
                null,
                Map.of(
                        "status", STATUS_FAILED,
                        "error", PROCESSING_STALE_ERROR,
                        "successCount", safeInt(job.getSuccessCount()),
                        "failureCount", safeInt(job.getFailureCount()),
                        "totalCount", safeInt(job.getTotalCount())
                )
        );
    }

    private boolean isStaleProcessing(ImportJobEntity job) {
        LocalDateTime startedAt = job.getStartedAt();
        if (startedAt == null) {
            return true;
        }
        return startedAt.isBefore(LocalDateTime.now().minusSeconds(importProcessingProperties.getStaleProcessingThresholdSeconds()));
    }

    private boolean hasProgress(ImportJobEntity job) {
        return safeInt(job.getTotalCount()) > 0
                || safeInt(job.getSuccessCount()) > 0
                || safeInt(job.getFailureCount()) > 0;
    }

    private ImportJobExecutionContext toExecutionContext(ImportJobEntity job) {
        return new ImportJobExecutionContext(
                job.getId(),
                job.getTenantId(),
                job.getImportType(),
                job.getStorageKey(),
                job.getRequestedBy(),
                job.getRequestId()
        );
    }

    @Transactional(noRollbackFor = RuntimeException.class)
    public void processChunk(ImportJobExecutionContext context, List<ImportJobChunkRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        ImportJobEntity job = requireJobForUpdate(context);
        if (!IMPORT_TYPE_USER_CSV.equals(job.getImportType())) {
            throw new IllegalStateException("unsupported import type for chunk processing: " + job.getImportType());
        }

        int totalDelta = 0;
        int successDelta = 0;
        int failureDelta = 0;
        RuntimeException unexpectedFailure = null;
        for (ImportJobChunkRow row : rows) {
            totalDelta++;
            if (row.columns().size() != ImportCsvSupport.USER_CSV_HEADERS.size()) {
                failureDelta++;
                saveError(job, row.rowNumber(), "INVALID_ROW_SHAPE", "column count mismatch", row.rawPayload());
                continue;
            }
            try {
                userCsvImportProcessor.processRow(job, row.rowNumber(), row.columns());
                successDelta++;
            } catch (ImportRowProcessingException ex) {
                failureDelta++;
                saveError(job, row.rowNumber(), ex.code(), ex.getMessage(), row.rawPayload());
            } catch (RuntimeException ex) {
                unexpectedFailure = ex;
                break;
            }
        }

        job.setTotalCount(safeInt(job.getTotalCount()) + totalDelta);
        job.setSuccessCount(safeInt(job.getSuccessCount()) + successDelta);
        job.setFailureCount(safeInt(job.getFailureCount()) + failureDelta);
        importJobRepository.save(job);
        if (unexpectedFailure != null) {
            throw unexpectedFailure;
        }
    }

    @Transactional
    public void completeJob(ImportJobExecutionContext context) {
        ImportJobEntity job = requireJobForUpdate(context);
        int success = safeInt(job.getSuccessCount());
        int failure = safeInt(job.getFailureCount());

        job.setFinishedAt(LocalDateTime.now());
        if (failure > 0 && success == 0) {
            job.setStatus(STATUS_FAILED);
            job.setErrorSummary("all rows failed validation");
            auditEventService.recordEvent(
                    job.getTenantId(),
                    "IMPORT_JOB",
                    job.getId(),
                    "IMPORT_JOB_FAILED",
                    job.getRequestedBy(),
                    job.getRequestId(),
                    null,
                    Map.of(
                            "status", STATUS_FAILED,
                            "failureCount", failure,
                            "totalCount", safeInt(job.getTotalCount())
                    )
            );
        } else {
            job.setStatus(STATUS_SUCCEEDED);
            job.setErrorSummary(failure > 0 ? "completed with some row errors" : null);
            auditEventService.recordEvent(
                    job.getTenantId(),
                    "IMPORT_JOB",
                    job.getId(),
                    "IMPORT_JOB_COMPLETED",
                    job.getRequestedBy(),
                    job.getRequestId(),
                    null,
                    Map.of(
                            "status", STATUS_SUCCEEDED,
                            "successCount", success,
                            "failureCount", failure,
                            "totalCount", safeInt(job.getTotalCount())
                    )
            );
        }
        importJobRepository.save(job);
    }

    @Transactional
    public void failJob(ImportJobExecutionContext context, ImportJobFailure failure) {
        ImportJobEntity job = requireJobForUpdate(context);
        saveError(job, failure.rowNumber(), failure.errorCode(), failure.errorMessage(), failure.rawPayload());
        job.setTotalCount(safeInt(job.getTotalCount()) + failure.totalCountDelta());
        job.setFailureCount(safeInt(job.getFailureCount()) + failure.failureCountDelta());
        job.setStatus(STATUS_FAILED);
        job.setFinishedAt(LocalDateTime.now());
        job.setErrorSummary(failure.errorSummary());
        importJobRepository.save(job);
        auditEventService.recordEvent(
                job.getTenantId(),
                "IMPORT_JOB",
                job.getId(),
                "IMPORT_JOB_FAILED",
                job.getRequestedBy(),
                job.getRequestId(),
                null,
                Map.of(
                        "status", STATUS_FAILED,
                        "error", failure.errorCode(),
                        "successCount", safeInt(job.getSuccessCount()),
                        "failureCount", safeInt(job.getFailureCount()),
                        "totalCount", safeInt(job.getTotalCount())
                )
        );
    }

    private ImportJobEntity requireJobForUpdate(ImportJobExecutionContext context) {
        return importJobRepository.findByIdAndTenantIdForUpdate(context.jobId(), context.tenantId())
                .orElseThrow(() -> new IllegalStateException("import job not found for execution"));
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private void saveError(ImportJobEntity job, Integer rowNumber, String code, String message, String rawPayload) {
        ImportJobItemErrorEntity error = new ImportJobItemErrorEntity();
        error.setTenantId(job.getTenantId());
        error.setImportJobId(job.getId());
        error.setRowNumber(rowNumber);
        error.setErrorCode(code);
        error.setErrorMessage(message);
        error.setRawPayload(rawPayload);
        error.setCreatedAt(LocalDateTime.now());
        importJobItemErrorRepository.save(error);
    }

    public record ImportJobExecutionContext(
            Long jobId,
            Long tenantId,
            String importType,
            String storageKey,
            Long requestedBy,
            String requestId
    ) {
    }

    public record ImportJobStartResult(
            ImportJobStartAction action,
            ImportJobExecutionContext context
    ) {
        public static ImportJobStartResult started(ImportJobExecutionContext context) {
            return new ImportJobStartResult(ImportJobStartAction.STARTED, context);
        }

        public static ImportJobStartResult ignore() {
            return new ImportJobStartResult(ImportJobStartAction.IGNORE, null);
        }

        public static ImportJobStartResult requeue() {
            return new ImportJobStartResult(ImportJobStartAction.REQUEUE, null);
        }
    }

    public enum ImportJobStartAction {
        STARTED,
        IGNORE,
        REQUEUE
    }

    public record ImportJobChunkRow(
            int rowNumber,
            List<String> columns,
            String rawPayload
    ) {
    }

    public record ImportJobFailure(
            Integer rowNumber,
            String errorCode,
            String errorMessage,
            String rawPayload,
            String errorSummary,
            int totalCountDelta,
            int failureCountDelta
    ) {
    }
}
