package com.renda.merchantops.api.dto.audit.query;

import java.time.LocalDateTime;

public record AuditEventResponse(
        Long id,
        String entityType,
        Long entityId,
        String actionType,
        Long operatorId,
        String requestId,
        String beforeValue,
        String afterValue,
        String approvalStatus,
        LocalDateTime createdAt
) {
}
