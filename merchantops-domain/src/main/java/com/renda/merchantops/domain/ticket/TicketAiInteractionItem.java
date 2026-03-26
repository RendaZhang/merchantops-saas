package com.renda.merchantops.domain.ticket;

import java.time.LocalDateTime;

public record TicketAiInteractionItem(
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
