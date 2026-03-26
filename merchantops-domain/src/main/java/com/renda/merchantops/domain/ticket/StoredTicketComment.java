package com.renda.merchantops.domain.ticket;

import java.time.LocalDateTime;

public record StoredTicketComment(
        Long id,
        Long ticketId,
        String content,
        Long createdBy,
        LocalDateTime createdAt
) {
}
