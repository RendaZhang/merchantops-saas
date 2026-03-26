package com.renda.merchantops.api.importjob.messaging;

record ImportJobFailure(
        Integer rowNumber,
        String errorCode,
        String errorMessage,
        String rawPayload,
        String errorSummary,
        int totalCountDelta,
        int failureCountDelta
) {
}
