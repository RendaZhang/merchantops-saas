package com.renda.merchantops.domain.importjob;

import java.time.LocalDateTime;

public record ImportJobAiInteractionItem(
        Long id,
        String interactionType,
        String status,
        String outputSummary,
        String promptVersion,
        String modelId,
        Long latencyMs,
        String requestId,
        Integer usagePromptTokens,
        Integer usageCompletionTokens,
        Integer usageTotalTokens,
        Long usageCostMicros,
        LocalDateTime createdAt
) {
}
