package com.renda.merchantops.domain.importjob;

import java.time.LocalDateTime;
import java.util.List;

public interface ImportJobQueryUseCase {

    ImportJobPageResult pageJobs(Long tenantId, ImportJobPageCriteria criteria);

    ImportJobDetail getJobDetail(Long tenantId, Long importJobId);

    ImportJobAiInteractionPageResult pageJobAiInteractions(Long tenantId,
                                                           Long importJobId,
                                                           ImportJobAiInteractionPageCriteria criteria);

    ImportJobErrorPageResult pageJobErrors(Long tenantId, Long importJobId, ImportJobErrorPageCriteria criteria);

    List<ImportJobRecord> listQueuedJobsForRecovery(LocalDateTime createdBefore, int limit);

    List<ImportJobRecord> listStaleProcessingJobsForRecovery(LocalDateTime startedBefore, int limit);
}
