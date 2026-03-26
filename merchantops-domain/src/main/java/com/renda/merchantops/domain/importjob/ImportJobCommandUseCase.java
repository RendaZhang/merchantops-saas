package com.renda.merchantops.domain.importjob;

public interface ImportJobCommandUseCase {

    ImportJobRecord createQueuedJob(Long tenantId, Long operatorId, String requestId, NewImportJobDraft draft);

    ImportJobRecord createReplayJob(Long tenantId, Long operatorId, String requestId, NewImportJobDraft draft);
}
