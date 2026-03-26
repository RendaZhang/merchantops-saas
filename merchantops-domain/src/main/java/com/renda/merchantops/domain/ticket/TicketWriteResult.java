package com.renda.merchantops.domain.ticket;

import java.time.LocalDateTime;

public record TicketWriteResult(
        Long id,
        Long tenantId,
        String title,
        String description,
        String status,
        Long assigneeId,
        String assigneeUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
