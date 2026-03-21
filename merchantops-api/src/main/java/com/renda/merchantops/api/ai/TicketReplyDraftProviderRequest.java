package com.renda.merchantops.api.ai;

public record TicketReplyDraftProviderRequest(
        String requestId,
        Long ticketId,
        String modelId,
        int timeoutMs,
        TicketReplyDraftPrompt prompt
) {
}
