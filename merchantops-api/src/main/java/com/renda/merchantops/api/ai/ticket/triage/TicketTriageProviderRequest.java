package com.renda.merchantops.api.ai.ticket.triage;

public record TicketTriageProviderRequest(
        String requestId,
        Long ticketId,
        String modelId,
        int timeoutMs,
        TicketTriagePrompt prompt
) {
}
