package com.renda.merchantops.api.ai.importjob.fixrecommendation;

import com.renda.merchantops.api.ai.core.AiUsageAwareResult;

import java.util.List;

public record ImportJobFixRecommendationProviderResult(
        String summary,
        List<RecommendedFix> recommendedFixes,
        List<String> confidenceNotes,
        List<String> recommendedOperatorChecks,
        String modelId,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        Long costMicros
) implements AiUsageAwareResult {

    public record RecommendedFix(
            String errorCode,
            String recommendedAction,
            String reasoning,
            boolean reviewRequired
    ) {
    }
}
