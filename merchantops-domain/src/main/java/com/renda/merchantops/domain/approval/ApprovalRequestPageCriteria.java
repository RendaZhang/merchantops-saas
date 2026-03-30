package com.renda.merchantops.domain.approval;

import java.util.Set;

public record ApprovalRequestPageCriteria(
        int page,
        int size,
        String status,
        String actionType,
        Long requestedBy,
        Set<String> allowedActionTypes
) {
}
