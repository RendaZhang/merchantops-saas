package com.renda.merchantops.api.dto.approval.query;

import java.time.LocalDateTime;

public record ApprovalRequestResponse(Long id,
                                      Long tenantId,
                                      String actionType,
                                      String entityType,
                                      Long entityId,
                                      Long requestedBy,
                                      Long reviewedBy,
                                      String status,
                                      String payloadJson,
                                      String requestId,
                                      LocalDateTime createdAt,
                                      LocalDateTime reviewedAt,
                                      LocalDateTime executedAt) {
}
