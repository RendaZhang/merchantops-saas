package com.renda.merchantops.api.dto.importjob.query;

import java.time.LocalDateTime;

public record ImportJobListItemResponse(
        Long id,
        String importType,
        String sourceType,
        String sourceFilename,
        String status,
        Long requestedBy,
        boolean hasFailures,
        Integer totalCount,
        Integer successCount,
        Integer failureCount,
        String errorSummary,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
