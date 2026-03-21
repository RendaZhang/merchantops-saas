package com.renda.merchantops.api.ai;

public record TicketReplyDraftProviderResult(
        String opening,
        String body,
        String nextStep,
        String closing,
        String modelId,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        Long costMicros
) {
}
