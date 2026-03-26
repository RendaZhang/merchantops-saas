package com.renda.merchantops.domain.ticket;

import java.time.LocalDateTime;

public record TicketOperationLogView(
        Long id,
        String operationType,
        String detail,
        Long operatorId,
        String operatorUsername,
        LocalDateTime createdAt
) {
}
