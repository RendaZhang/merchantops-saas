package com.renda.merchantops.api.ai.client;

import java.util.Map;

public record StructuredOutputAiRequest(
        String requestId,
        String modelId,
        int timeoutMs,
        String systemPrompt,
        String userPrompt,
        int maxOutputTokens,
        String schemaName,
        Map<String, Object> schema,
        String exampleJson
) {
}
