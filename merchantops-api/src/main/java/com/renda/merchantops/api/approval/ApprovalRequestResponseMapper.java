package com.renda.merchantops.api.approval;

import com.renda.merchantops.api.dto.approval.query.ApprovalRequestListItemResponse;
import com.renda.merchantops.api.dto.approval.query.ApprovalRequestResponse;
import com.renda.merchantops.domain.approval.ApprovalRequestRecord;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
class ApprovalRequestResponseMapper {

    ApprovalRequestListItemResponse toListItemResponse(ApprovalRequestRecord record) {
        return new ApprovalRequestListItemResponse(
                record.id(),
                record.actionType(),
                record.entityType(),
                record.entityId(),
                record.requestedBy(),
                record.reviewedBy(),
                record.status(),
                record.createdAt(),
                record.reviewedAt(),
                record.executedAt()
        );
    }

    ApprovalRequestResponse toResponse(ApprovalRequestRecord record) {
        return new ApprovalRequestResponse(
                record.id(),
                record.tenantId(),
                record.actionType(),
                record.entityType(),
                record.entityId(),
                record.requestedBy(),
                record.reviewedBy(),
                record.status(),
                record.payloadJson(),
                record.requestId(),
                record.createdAt(),
                record.reviewedAt(),
                record.executedAt()
        );
    }

    Map<String, Object> snapshot(ApprovalRequestRecord record) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", record.id());
        snapshot.put("actionType", record.actionType());
        snapshot.put("entityType", record.entityType());
        snapshot.put("entityId", record.entityId());
        snapshot.put("status", record.status());
        snapshot.put("requestedBy", record.requestedBy());
        snapshot.put("reviewedBy", record.reviewedBy());
        snapshot.put("payloadJson", record.payloadJson());
        snapshot.put("createdAt", record.createdAt());
        snapshot.put("reviewedAt", record.reviewedAt());
        snapshot.put("executedAt", record.executedAt());
        return snapshot;
    }
}
