package com.renda.merchantops.api.ai;

public record AiInteractionRecordCommand(
        Long tenantId,
        Long userId,
        String requestId,
        String entityType,
        Long entityId,
        String interactionType,
        String promptVersion,
        String modelId,
        AiInteractionStatus status,
        long latencyMs,
        String outputSummary,
        Integer usagePromptTokens,
        Integer usageCompletionTokens,
        Integer usageTotalTokens,
        Long usageCostMicros
) {
}
