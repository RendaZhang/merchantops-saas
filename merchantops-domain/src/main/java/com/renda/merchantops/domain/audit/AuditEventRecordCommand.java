package com.renda.merchantops.domain.audit;

public record AuditEventRecordCommand(
        Long tenantId,
        String entityType,
        Long entityId,
        String actionType,
        Long operatorId,
        String requestId,
        String beforeValue,
        String afterValue
) {
}
