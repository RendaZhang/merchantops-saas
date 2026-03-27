package com.renda.merchantops.domain.importjob;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;

import java.time.LocalDateTime;

public class ImportJobQueryDomainService implements ImportJobQueryUseCase {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final int DEFAULT_RECOVERY_BATCH_SIZE = 100;

    private final ImportJobQueryPort importJobQueryPort;

    public ImportJobQueryDomainService(ImportJobQueryPort importJobQueryPort) {
        this.importJobQueryPort = importJobQueryPort;
    }

    @Override
    public ImportJobPageResult pageJobs(Long tenantId, ImportJobPageCriteria criteria) {
        return importJobQueryPort.pageJobs(tenantId, normalize(criteria));
    }

    @Override
    public ImportJobDetail getJobDetail(Long tenantId, Long importJobId) {
        ImportJobRecord job = importJobQueryPort.findJob(tenantId, importJobId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "import job not found"));
        return new ImportJobDetail(
                job,
                importJobQueryPort.summarizeErrorCodes(job.tenantId(), job.id()),
                importJobQueryPort.listJobErrors(job.tenantId(), job.id())
        );
    }

    @Override
    public ImportJobErrorPageResult pageJobErrors(Long tenantId,
                                                  Long importJobId,
                                                  ImportJobErrorPageCriteria criteria) {
        ImportJobRecord job = importJobQueryPort.findJob(tenantId, importJobId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "import job not found"));
        return importJobQueryPort.pageJobErrors(job.tenantId(), job.id(), normalize(criteria));
    }

    @Override
    public java.util.List<ImportJobRecord> listQueuedJobsForRecovery(LocalDateTime createdBefore, int limit) {
        LocalDateTime resolvedCreatedBefore = createdBefore == null ? LocalDateTime.now() : createdBefore;
        int resolvedLimit = limit <= 0 ? DEFAULT_RECOVERY_BATCH_SIZE : limit;
        return importJobQueryPort.findQueuedJobsForRecovery(resolvedCreatedBefore, resolvedLimit);
    }

    @Override
    public java.util.List<ImportJobRecord> listStaleProcessingJobsForRecovery(LocalDateTime startedBefore, int limit) {
        LocalDateTime resolvedStartedBefore = startedBefore == null ? LocalDateTime.now() : startedBefore;
        int resolvedLimit = limit <= 0 ? DEFAULT_RECOVERY_BATCH_SIZE : limit;
        return importJobQueryPort.findStaleProcessingJobsForRecovery(resolvedStartedBefore, resolvedLimit);
    }

    private ImportJobPageCriteria normalize(ImportJobPageCriteria criteria) {
        if (criteria == null) {
            return new ImportJobPageCriteria(DEFAULT_PAGE, DEFAULT_SIZE, null, null, null, false);
        }
        return new ImportJobPageCriteria(
                normalizePage(criteria.page()),
                normalizeSize(criteria.size()),
                normalizeFilter(criteria.status()),
                normalizeFilter(criteria.importType()),
                criteria.requestedBy(),
                criteria.hasFailuresOnly()
        );
    }

    private ImportJobErrorPageCriteria normalize(ImportJobErrorPageCriteria criteria) {
        if (criteria == null) {
            return new ImportJobErrorPageCriteria(DEFAULT_PAGE, DEFAULT_SIZE, null);
        }
        return new ImportJobErrorPageCriteria(
                normalizePage(criteria.page()),
                normalizeSize(criteria.size()),
                normalizeFilter(criteria.errorCode())
        );
    }

    private int normalizePage(int page) {
        return page < 0 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
