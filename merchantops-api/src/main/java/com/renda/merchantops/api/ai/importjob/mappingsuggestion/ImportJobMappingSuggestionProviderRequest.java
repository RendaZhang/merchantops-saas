package com.renda.merchantops.api.ai.importjob.mappingsuggestion;

public record ImportJobMappingSuggestionProviderRequest(
        String requestId,
        Long importJobId,
        String modelId,
        int timeoutMs,
        ImportJobMappingSuggestionPrompt prompt
) {
}
