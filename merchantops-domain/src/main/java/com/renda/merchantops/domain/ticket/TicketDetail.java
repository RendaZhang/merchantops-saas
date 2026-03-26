package com.renda.merchantops.domain.ticket;

import java.time.LocalDateTime;
import java.util.List;

public record TicketDetail(
        Long id,
        Long tenantId,
        String title,
        String description,
        String status,
        Long assigneeId,
        String assigneeUsername,
        Long createdBy,
        String createdByUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TicketCommentView> comments,
        List<TicketOperationLogView> operationLogs
) {
}
