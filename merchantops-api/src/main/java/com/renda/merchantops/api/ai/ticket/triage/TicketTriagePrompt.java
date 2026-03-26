package com.renda.merchantops.api.ai.ticket.triage;

public record TicketTriagePrompt(
        String promptVersion,
        String systemPrompt,
        String userPrompt
) {
}
