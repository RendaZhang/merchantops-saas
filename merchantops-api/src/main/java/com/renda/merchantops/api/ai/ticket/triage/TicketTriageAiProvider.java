package com.renda.merchantops.api.ai.ticket.triage;

public interface TicketTriageAiProvider {

    TicketTriageProviderResult generateTriage(TicketTriageProviderRequest request);
}
