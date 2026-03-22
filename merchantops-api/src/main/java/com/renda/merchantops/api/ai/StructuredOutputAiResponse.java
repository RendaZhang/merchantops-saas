package com.renda.merchantops.api.ai;

public record StructuredOutputAiResponse(
        String rawText,
        String modelId,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        Long costMicros
) {
}
