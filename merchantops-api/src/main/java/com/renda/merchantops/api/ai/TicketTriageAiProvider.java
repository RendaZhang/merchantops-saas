package com.renda.merchantops.api.ai;

public interface TicketTriageAiProvider {

    TicketTriageProviderResult generateTriage(TicketTriageProviderRequest request);
}
