package com.renda.merchantops.domain.ai;

public record AiInteractionUsageByStatus(
        String status,
        Long count,
        Long totalTokens,
        Long totalCostMicros
) {
}
