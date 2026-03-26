package com.renda.merchantops.api.ai.ticket.summary;

public interface TicketSummaryAiProvider {

    TicketSummaryProviderResult generateSummary(TicketSummaryProviderRequest request);
}
