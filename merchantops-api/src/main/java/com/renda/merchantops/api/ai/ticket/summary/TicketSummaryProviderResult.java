package com.renda.merchantops.api.ai.ticket.summary;

import com.renda.merchantops.api.ai.core.AiUsageAwareResult;

public record TicketSummaryProviderResult(
        String summary,
        String modelId,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        Long costMicros
) implements AiUsageAwareResult {
}
