package com.renda.merchantops.api.ai;

public record TicketSummaryProviderRequest(
        String requestId,
        Long ticketId,
        String modelId,
        int timeoutMs,
        TicketSummaryPrompt prompt
) {
}
