package com.renda.merchantops.domain.ticket;

import java.util.List;

public record TicketPageResult(
        List<TicketListItem> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}
