package com.renda.merchantops.domain.ai;

import java.time.LocalDateTime;

public record NewAiInteractionRecord(
        Long tenantId,
        Long userId,
        String requestId,
        String entityType,
        Long entityId,
        String interactionType,
        String promptVersion,
        String modelId,
        String status,
        Long latencyMs,
        String outputSummary,
        Integer usagePromptTokens,
        Integer usageCompletionTokens,
        Integer usageTotalTokens,
        Long usageCostMicros,
        LocalDateTime createdAt
) {
}
