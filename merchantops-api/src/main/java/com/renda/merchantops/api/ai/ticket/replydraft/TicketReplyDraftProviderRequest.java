package com.renda.merchantops.api.ai.ticket.replydraft;

public record TicketReplyDraftProviderRequest(
        String requestId,
        Long ticketId,
        String modelId,
        int timeoutMs,
        TicketReplyDraftPrompt prompt
) {
}
