package com.renda.merchantops.domain.approval;

import java.util.Optional;

public interface ApprovalRequestPort {

    boolean existsPendingDisableRequest(Long tenantId, Long userId);

    ApprovalRequestRecord save(ApprovalRequestRecord request);

    Optional<ApprovalRequestRecord> findById(Long tenantId, Long approvalRequestId);

    Optional<ApprovalRequestRecord> findByIdForUpdate(Long tenantId, Long approvalRequestId);

    ApprovalRequestPageResult page(Long tenantId, ApprovalRequestPageCriteria criteria);
}
