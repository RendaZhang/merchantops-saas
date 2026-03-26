package com.renda.merchantops.domain.ticket;

import java.time.LocalDateTime;

public record NewTicketCommentDraft(
        Long tenantId,
        Long ticketId,
        String content,
        Long createdBy,
        String requestId,
        LocalDateTime createdAt
) {
}
