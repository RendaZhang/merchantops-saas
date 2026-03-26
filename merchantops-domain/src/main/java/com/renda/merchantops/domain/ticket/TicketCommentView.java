package com.renda.merchantops.domain.ticket;

import java.time.LocalDateTime;

public record TicketCommentView(
        Long id,
        Long ticketId,
        String content,
        Long createdBy,
        String createdByUsername,
        LocalDateTime createdAt
) {
}
