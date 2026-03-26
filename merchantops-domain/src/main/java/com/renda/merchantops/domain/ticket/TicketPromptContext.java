package com.renda.merchantops.domain.ticket;

import java.time.LocalDateTime;
import java.util.List;

public record TicketPromptContext(
        Long id,
        Long tenantId,
        String title,
        String description,
        String status,
        String assigneeUsername,
        String createdByUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<Comment> comments,
        boolean olderCommentsOmitted,
        List<OperationLog> operationLogs,
        boolean olderOperationLogsOmitted
) {

    public record Comment(
            Long id,
            String content,
            String createdByUsername,
            LocalDateTime createdAt
    ) {
    }

    public record OperationLog(
            Long id,
            String operationType,
            String detail,
            String operatorUsername,
            LocalDateTime createdAt
    ) {
    }
}
