package com.renda.merchantops.domain.ticket;

import java.time.LocalDateTime;

public record NewTicketOperationLogDraft(
        Long ticketId,
        Long tenantId,
        Long operatorId,
        String requestId,
        String operationType,
        String detail,
        LocalDateTime createdAt
) {
}
