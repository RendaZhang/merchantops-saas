package com.renda.merchantops.api.ai.importjob.fixrecommendation;

public record ImportJobFixRecommendationProviderRequest(
        String requestId,
        Long importJobId,
        String modelId,
        int timeoutMs,
        ImportJobFixRecommendationPrompt prompt
) {
}
