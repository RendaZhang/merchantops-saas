package com.renda.merchantops.api.ai;

public interface TicketSummaryAiProvider {

    TicketSummaryProviderResult generateSummary(TicketSummaryProviderRequest request);
}
