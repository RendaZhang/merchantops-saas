package com.renda.merchantops.domain.approval;

import java.util.List;

public record ApprovalRequestPageResult(
        List<ApprovalRequestRecord> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}
