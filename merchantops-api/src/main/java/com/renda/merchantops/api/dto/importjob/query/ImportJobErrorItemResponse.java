package com.renda.merchantops.api.dto.importjob.query;

import java.time.LocalDateTime;

public record ImportJobErrorItemResponse(
        Long id,
        Integer rowNumber,
        String errorCode,
        String errorMessage,
        String rawPayload,
        LocalDateTime createdAt
) {
}
