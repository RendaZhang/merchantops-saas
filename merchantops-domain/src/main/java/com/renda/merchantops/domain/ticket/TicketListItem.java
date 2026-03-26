package com.renda.merchantops.domain.ticket;

import java.time.LocalDateTime;

public record TicketListItem(
        Long id,
        String title,
        String status,
        Long assigneeId,
        String assigneeUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
