package com.renda.merchantops.domain.ticket;

import java.util.List;

public record TicketAiInteractionPageResult(
        List<TicketAiInteractionItem> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}
