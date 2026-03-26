package com.renda.merchantops.api.ai.ticket.replydraft;

public record TicketReplyDraftPrompt(
        String promptVersion,
        String systemPrompt,
        String userPrompt
) {
}
