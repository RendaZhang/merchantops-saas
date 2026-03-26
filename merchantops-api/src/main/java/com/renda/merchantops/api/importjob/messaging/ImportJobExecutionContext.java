package com.renda.merchantops.api.importjob.messaging;

record ImportJobExecutionContext(
        Long jobId,
        Long tenantId,
        String importType,
        String storageKey,
        Long requestedBy,
        String requestId
) {
}
