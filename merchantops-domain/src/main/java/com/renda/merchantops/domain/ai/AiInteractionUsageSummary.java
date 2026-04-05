package com.renda.merchantops.domain.ai;

import java.time.LocalDateTime;
import java.util.List;

public record AiInteractionUsageSummary(
        LocalDateTime from,
        LocalDateTime to,
        Long totalInteractions,
        Long succeededCount,
        Long failedCount,
        Long totalPromptTokens,
        Long totalCompletionTokens,
        Long totalTokens,
        Long totalCostMicros,
        List<AiInteractionUsageByInteractionType> byInteractionType,
        List<AiInteractionUsageByStatus> byStatus
) {
}
