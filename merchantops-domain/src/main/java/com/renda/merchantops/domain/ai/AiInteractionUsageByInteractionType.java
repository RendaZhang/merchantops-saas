package com.renda.merchantops.domain.ai;

public record AiInteractionUsageByInteractionType(
        String interactionType,
        Long count,
        Long succeededCount,
        Long failedCount,
        Long totalTokens,
        Long totalCostMicros
) {
}
