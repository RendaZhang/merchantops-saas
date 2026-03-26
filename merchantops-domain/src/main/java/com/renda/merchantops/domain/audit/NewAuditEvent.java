package com.renda.merchantops.domain.audit;

import java.time.LocalDateTime;

public record NewAuditEvent(
        Long tenantId,
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
