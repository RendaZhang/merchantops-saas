package com.renda.merchantops.domain.ticket;

public record TicketAiInteractionPageCriteria(
        int page,
        int size,
        String interactionType,
        String status
) {
}
