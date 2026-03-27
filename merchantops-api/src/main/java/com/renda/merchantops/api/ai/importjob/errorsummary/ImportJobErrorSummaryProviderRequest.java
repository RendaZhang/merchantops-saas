package com.renda.merchantops.api.ai.importjob.errorsummary;

public record ImportJobErrorSummaryProviderRequest(
        String requestId,
        Long importJobId,
        String modelId,
        int timeoutMs,
        ImportJobErrorSummaryPrompt prompt
) {
}
