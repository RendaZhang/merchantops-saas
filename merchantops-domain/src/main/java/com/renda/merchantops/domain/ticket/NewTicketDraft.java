package com.renda.merchantops.domain.ticket;

import java.time.LocalDateTime;

public record NewTicketDraft(
        Long tenantId,
        String title,
        String description,
        String status,
        Long createdBy,
        String requestId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
