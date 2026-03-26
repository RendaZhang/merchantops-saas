package com.renda.merchantops.api.ai.ticket.replydraft;

public interface TicketReplyDraftAiProvider {

    TicketReplyDraftProviderResult generateReplyDraft(TicketReplyDraftProviderRequest request);
}
