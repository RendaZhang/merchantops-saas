package com.renda.merchantops.api.importjob.messaging;

import com.renda.merchantops.api.audit.AuditEventService;
import com.renda.merchantops.api.config.ImportProcessingProperties;
import com.renda.merchantops.domain.importjob.ImportJobCommandPort;
import com.renda.merchantops.domain.importjob.ImportJobRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportJobExecutionCoordinator {

    private static final String STATUS_QUEUED = "QUEUED";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SUCCEEDED = "SUCCEEDED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String PROCESSING_STALE_ERROR = "PROCESSING_STALE";
    private static final String PROCESSING_STALE_ERROR_SUMMARY = "import job processing expired after partial progress";

    private final ImportJobCommandPort importJobCommandPort;
    private final AuditEventService auditEventService;
    private final ImportJobChunkProcessor importJobChunkProcessor;
    private final ImportJobFailureRecorder importJobFailureRecorder;
    private final ImportProcessingProperties importProcessingProperties;

    @Transactional
    public ImportJobStartResult startProcessing(Long jobId, Long tenantId) {
        if (jobId == null || tenantId == null) {
            return ImportJobStartResult.ignore();
        }
        ImportJobRecord job = importJobCommandPort.findJobForUpdate(tenantId, jobId).orElse(null);
        if (job == null) {
            return ImportJobStartResult.ignore();
        }
        if (STATUS_QUEUED.equals(job.status())) {
            return ImportJobStartResult.started(transitionToProcessing(job, false));
        }
        if (STATUS_PROCESSING.equals(job.status())) {
            return recoverOrFailStaleProcessing(job);
        }
        return ImportJobStartResult.ignore();
    }

    @Transactional(noRollbackFor = RuntimeException.class)
    public boolean processChunk(ImportJobExecutionContext context, List<ImportJobChunkRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return true;
        }
        Optional<ImportJobRecord> job = findProcessingJobForUpdate(context, "chunk execution");
        if (job.isEmpty()) {
            return false;
        }
        importJobChunkProcessor.processChunk(job.get(), context, rows);
        return true;
    }

    @Transactional
    public void completeJob(ImportJobExecutionContext context) {
        Optional<ImportJobRecord> maybeJob = findProcessingJobForUpdate(context, "completion");
        if (maybeJob.isEmpty()) {
            return;
        }
        ImportJobRecord job = maybeJob.get();
        int success = safeInt(job.successCount());
        int failure = safeInt(job.failureCount());
        LocalDateTime finishedAt = LocalDateTime.now();

        if (failure > 0 && success == 0) {
            ImportJobRecord saved = importJobCommandPort.saveJob(new ImportJobRecord(
                    job.id(),
                    job.tenantId(),
                    job.importType(),
                    job.sourceType(),
                    job.sourceFilename(),
                    job.storageKey(),
                    job.sourceJobId(),
                    STATUS_FAILED,
                    job.requestedBy(),
                    job.requestId(),
                    job.totalCount(),
                    job.successCount(),
                    job.failureCount(),
                    "all rows failed validation",
                    job.createdAt(),
                    job.startedAt(),
                    finishedAt
            ));
            importJobFailureRecorder.recordJobFailure(saved, null);
            return;
        }

        ImportJobRecord saved = importJobCommandPort.saveJob(new ImportJobRecord(
                job.id(),
                job.tenantId(),
                job.importType(),
                job.sourceType(),
                job.sourceFilename(),
                job.storageKey(),
                job.sourceJobId(),
                STATUS_SUCCEEDED,
                job.requestedBy(),
                job.requestId(),
                job.totalCount(),
                job.successCount(),
                job.failureCount(),
                failure > 0 ? "completed with some row errors" : null,
                job.createdAt(),
                job.startedAt(),
                finishedAt
        ));
        auditEventService.recordEvent(
                saved.tenantId(),
                "IMPORT_JOB",
                saved.id(),
                "IMPORT_JOB_COMPLETED",
                saved.requestedBy(),
                saved.requestId(),
                null,
                Map.of(
                        "status", STATUS_SUCCEEDED,
                        "successCount", success,
                        "failureCount", failure,
                        "totalCount", safeInt(saved.totalCount())
                )
        );
    }

    @Transactional
    public void failJob(ImportJobExecutionContext context, ImportJobFailure failure) {
        Optional<ImportJobRecord> maybeJob = findProcessingJobForUpdate(context, "failure handling");
        if (maybeJob.isEmpty()) {
            return;
        }
        ImportJobRecord job = maybeJob.get();
        if (failure.rowNumber() != null || failure.errorCode() != null || failure.errorMessage() != null
                || failure.rawPayload() != null) {
            importJobFailureRecorder.saveRowError(
                    job,
                    failure.rowNumber(),
                    failure.errorCode(),
                    failure.errorMessage(),
                    failure.rawPayload()
            );
        }
        ImportJobRecord saved = importJobCommandPort.saveJob(new ImportJobRecord(
                job.id(),
                job.tenantId(),
                job.importType(),
                job.sourceType(),
                job.sourceFilename(),
                job.storageKey(),
                job.sourceJobId(),
                STATUS_FAILED,
                job.requestedBy(),
                job.requestId(),
                safeInt(job.totalCount()) + failure.totalCountDelta(),
                job.successCount(),
                safeInt(job.failureCount()) + failure.failureCountDelta(),
                failure.errorSummary(),
                job.createdAt(),
                job.startedAt(),
                LocalDateTime.now()
        ));
        importJobFailureRecorder.recordJobFailure(saved, failure.errorCode());
    }

    private ImportJobStartResult recoverOrFailStaleProcessing(ImportJobRecord job) {
        if (!isStaleProcessing(job)) {
            return ImportJobStartResult.requeue();
        }
        // Once counters move, resuming the same job risks double-processing rows and skewing
        // totals, so stale jobs with progress are failed instead of silently resumed.
        if (hasProgress(job)) {
            failStaleProcessingJob(job);
            return ImportJobStartResult.ignore();
        }
        // A stale job with no progress is safe to restart because nothing observable has been
        // persisted beyond the job header itself.
        return ImportJobStartResult.started(transitionToProcessing(job, true));
    }

    private ImportJobExecutionContext transitionToProcessing(ImportJobRecord job, boolean recoveredFromStale) {
        LocalDateTime now = LocalDateTime.now();
        ImportJobRecord saved = importJobCommandPort.saveJob(new ImportJobRecord(
                job.id(),
                job.tenantId(),
                job.importType(),
                job.sourceType(),
                job.sourceFilename(),
                job.storageKey(),
                job.sourceJobId(),
                STATUS_PROCESSING,
                job.requestedBy(),
                job.requestId(),
                job.totalCount(),
                job.successCount(),
                job.failureCount(),
                job.errorSummary(),
                job.createdAt(),
                now,
                null
        ));
        Map<String, Object> afterValue = new LinkedHashMap<>();
        afterValue.put("status", STATUS_PROCESSING);
        if (recoveredFromStale) {
            afterValue.put("recoveredFromStale", true);
        }
        auditEventService.recordEvent(
                saved.tenantId(),
                "IMPORT_JOB",
                saved.id(),
                "IMPORT_JOB_PROCESSING_STARTED",
                saved.requestedBy(),
                saved.requestId(),
                null,
                afterValue
        );
        return new ImportJobExecutionContext(
                saved.id(),
                saved.tenantId(),
                saved.importType(),
                saved.storageKey(),
                saved.requestedBy(),
                saved.requestId()
        );
    }

    private void failStaleProcessingJob(ImportJobRecord job) {
        ImportJobRecord saved = importJobCommandPort.saveJob(new ImportJobRecord(
                job.id(),
                job.tenantId(),
                job.importType(),
                job.sourceType(),
                job.sourceFilename(),
                job.storageKey(),
                job.sourceJobId(),
                STATUS_FAILED,
                job.requestedBy(),
                job.requestId(),
                job.totalCount(),
                job.successCount(),
                job.failureCount(),
                PROCESSING_STALE_ERROR_SUMMARY,
                job.createdAt(),
                job.startedAt(),
                LocalDateTime.now()
        ));
        importJobFailureRecorder.recordJobFailure(saved, PROCESSING_STALE_ERROR);
    }

    private boolean isStaleProcessing(ImportJobRecord job) {
        LocalDateTime startedAt = job.startedAt();
        if (startedAt == null) {
            // Treat missing startedAt as stale so queue recovery can heal partially-written
            // PROCESSING rows instead of leaving them stuck forever.
            return true;
        }
        return startedAt.isBefore(LocalDateTime.now().minusSeconds(importProcessingProperties.getStaleProcessingThresholdSeconds()));
    }

    private boolean hasProgress(ImportJobRecord job) {
        return safeInt(job.totalCount()) > 0
                || safeInt(job.successCount()) > 0
                || safeInt(job.failureCount()) > 0;
    }

    private Optional<ImportJobRecord> findProcessingJobForUpdate(ImportJobExecutionContext context, String operation) {
        Optional<ImportJobRecord> maybeJob = importJobCommandPort.findJobForUpdate(context.tenantId(), context.jobId());
        if (maybeJob.isEmpty()) {
            log.debug("skipping import job {} {} because job no longer exists", context.jobId(), operation);
            return Optional.empty();
        }
        ImportJobRecord job = maybeJob.get();
        if (!STATUS_PROCESSING.equals(job.status())) {
            log.debug("skipping import job {} {} because status is {}", context.jobId(), operation, job.status());
            return Optional.empty();
        }
        return Optional.of(job);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
