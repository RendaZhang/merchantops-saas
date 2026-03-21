package com.renda.merchantops.api.ai;

public record TicketTriagePrompt(
        String promptVersion,
        String systemPrompt,
        String userPrompt
) {
}
