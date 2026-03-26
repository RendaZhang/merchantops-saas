package com.renda.merchantops.domain.ticket;

public record TicketPageCriteria(
        int page,
        int size,
        String status,
        Long assigneeId,
        String keyword,
        boolean unassignedOnly
) {
}
