package com.renda.merchantops.domain.approval;

import java.util.Optional;

public interface ApprovalRequestPort {

    ApprovalRequestRecord save(ApprovalRequestRecord request);

    boolean existsPendingRequestKey(Long tenantId, String pendingRequestKey);

    Optional<ApprovalRequestRecord> findById(Long tenantId, Long approvalRequestId);

    Optional<ApprovalRequestRecord> findByIdForUpdate(Long tenantId, Long approvalRequestId);

    ApprovalRequestPageResult page(Long tenantId, ApprovalRequestPageCriteria criteria);
}
