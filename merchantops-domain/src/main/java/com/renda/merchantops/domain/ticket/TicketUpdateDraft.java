package com.renda.merchantops.domain.ticket;

import java.time.LocalDateTime;

public record TicketUpdateDraft(
        Long id,
        Long tenantId,
        String title,
        String description,
        String status,
        Long assigneeId,
        Long createdBy,
        String requestId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
