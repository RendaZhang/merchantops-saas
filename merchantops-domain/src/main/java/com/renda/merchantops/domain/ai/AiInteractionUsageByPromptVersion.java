package com.renda.merchantops.domain.ai;

public record AiInteractionUsageByPromptVersion(
        String promptVersion,
        Long count,
        Long succeededCount,
        Long failedCount,
        Long totalTokens,
        Long totalCostMicros
) {
}
