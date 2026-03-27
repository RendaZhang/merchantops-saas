package com.renda.merchantops.api.ai.importjob.errorsummary;

import com.renda.merchantops.api.ai.core.AiUsageAwareResult;

import java.util.List;

public record ImportJobErrorSummaryProviderResult(
        String summary,
        List<String> topErrorPatterns,
        List<String> recommendedNextSteps,
        String modelId,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        Long costMicros
) implements AiUsageAwareResult {
}
