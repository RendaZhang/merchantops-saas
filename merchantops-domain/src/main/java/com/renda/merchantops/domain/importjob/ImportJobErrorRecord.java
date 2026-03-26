package com.renda.merchantops.domain.importjob;

import java.time.LocalDateTime;

public record ImportJobErrorRecord(
        Long id,
        Long tenantId,
        Long importJobId,
        Integer rowNumber,
        String errorCode,
        String errorMessage,
        String rawPayload,
        LocalDateTime createdAt
) {
}
