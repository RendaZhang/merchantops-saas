package com.renda.merchantops.api.ai;

public interface TicketReplyDraftAiProvider {

    TicketReplyDraftProviderResult generateReplyDraft(TicketReplyDraftProviderRequest request);
}
