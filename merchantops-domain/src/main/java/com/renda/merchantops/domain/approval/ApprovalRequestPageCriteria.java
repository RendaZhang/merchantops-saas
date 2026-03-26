package com.renda.merchantops.domain.approval;

public record ApprovalRequestPageCriteria(
        int page,
        int size,
        String status,
        String actionType,
        Long requestedBy
) {
}
