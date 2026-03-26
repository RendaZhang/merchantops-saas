package com.renda.merchantops.api.ai.ticket.replydraft;

import com.renda.merchantops.api.ai.core.AiUsageAwareResult;

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
) implements AiUsageAwareResult {
}
