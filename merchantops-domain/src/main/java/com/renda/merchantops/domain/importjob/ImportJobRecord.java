package com.renda.merchantops.domain.importjob;

import java.time.LocalDateTime;

public record ImportJobRecord(
        Long id,
        Long tenantId,
        String importType,
        String sourceType,
        String sourceFilename,
        String storageKey,
        Long sourceJobId,
        String status,
        Long requestedBy,
        String requestId,
        Integer totalCount,
        Integer successCount,
        Integer failureCount,
        String errorSummary,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
