package com.renda.merchantops.api.ai;

public record TicketTriageProviderRequest(
        String requestId,
        Long ticketId,
        String modelId,
        int timeoutMs,
        TicketTriagePrompt prompt
) {
}
