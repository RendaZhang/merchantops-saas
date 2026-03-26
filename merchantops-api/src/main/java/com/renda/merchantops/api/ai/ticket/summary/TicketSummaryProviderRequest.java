package com.renda.merchantops.api.ai.ticket.summary;

public record TicketSummaryProviderRequest(
        String requestId,
        Long ticketId,
        String modelId,
        int timeoutMs,
        TicketSummaryPrompt prompt
) {
}
