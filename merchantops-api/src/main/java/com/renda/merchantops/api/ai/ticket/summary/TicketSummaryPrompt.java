package com.renda.merchantops.api.ai.ticket.summary;

public record TicketSummaryPrompt(
        String promptVersion,
        String systemPrompt,
        String userPrompt
) {
}
