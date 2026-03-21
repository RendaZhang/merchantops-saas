package com.renda.merchantops.api.ai;

public record TicketReplyDraftPrompt(
        String promptVersion,
        String systemPrompt,
        String userPrompt
) {
}
