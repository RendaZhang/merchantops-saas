package com.renda.merchantops.api.ai;

public record TicketSummaryProviderResult(
        String summary,
        String modelId,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        Long costMicros
) {
}
