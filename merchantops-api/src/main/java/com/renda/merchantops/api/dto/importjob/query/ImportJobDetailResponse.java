package com.renda.merchantops.api.dto.importjob.query;

import java.time.LocalDateTime;
import java.util.List;

public record ImportJobDetailResponse(
        Long id,
        Long tenantId,
        String importType,
        String sourceType,
        String sourceFilename,
        String storageKey,
        String status,
        Long requestedBy,
        String requestId,
        Integer totalCount,
        Integer successCount,
        Integer failureCount,
        String errorSummary,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        List<ImportJobErrorItemResponse> itemErrors
) {
}
