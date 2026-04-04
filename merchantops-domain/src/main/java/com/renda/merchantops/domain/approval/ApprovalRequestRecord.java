package com.renda.merchantops.domain.approval;

import java.time.LocalDateTime;

public record ApprovalRequestRecord(
        Long id,
        Long tenantId,
        String actionType,
        String entityType,
        Long entityId,
        Long requestedBy,
        Long reviewedBy,
        String status,
        String payloadJson,
        String pendingRequestKey,
        String requestId,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        LocalDateTime executedAt
) {
}
