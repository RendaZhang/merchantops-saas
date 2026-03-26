package com.renda.merchantops.domain.audit;

import java.time.LocalDateTime;

public record AuditEventItem(
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
