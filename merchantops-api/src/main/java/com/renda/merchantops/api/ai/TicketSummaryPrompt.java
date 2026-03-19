package com.renda.merchantops.api.ai;

public record TicketSummaryPrompt(
        String promptVersion,
        String systemPrompt,
        String userPrompt
) {
}
