package com.renda.merchantops.api.ai.client;

import com.renda.merchantops.api.ai.core.AiUsageAwareResult;

public record StructuredOutputAiResponse(
        String rawText,
        String modelId,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        Long costMicros
) implements AiUsageAwareResult {
}
