package com.renda.merchantops.domain.importjob;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ImportJobQueryPort {

    ImportJobPageResult pageJobs(Long tenantId, ImportJobPageCriteria criteria);

    Optional<ImportJobRecord> findJob(Long tenantId, Long importJobId);

    ImportJobErrorPageResult pageJobErrors(Long tenantId, Long importJobId, ImportJobErrorPageCriteria criteria);

    List<ImportJobErrorCount> summarizeErrorCodes(Long tenantId, Long importJobId);

    List<ImportJobErrorRecord> listJobErrors(Long tenantId, Long importJobId);

    List<ImportJobRecord> findQueuedJobsForRecovery(LocalDateTime createdBefore, int limit);

    List<ImportJobRecord> findStaleProcessingJobsForRecovery(LocalDateTime startedBefore, int limit);
}
