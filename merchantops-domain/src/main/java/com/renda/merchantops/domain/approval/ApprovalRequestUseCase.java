package com.renda.merchantops.domain.approval;

public interface ApprovalRequestUseCase {

    ApprovalRequestRecord createDisableRequest(Long tenantId, Long requestedBy, String requestId, Long userId);

    ApprovalRequestRecord createImportSelectiveReplayRequest(Long tenantId,
                                                             Long requestedBy,
                                                             String requestId,
                                                             ImportSelectiveReplayApprovalCommand command);

    ApprovalRequestRecord getById(Long tenantId, Long approvalRequestId);

    ApprovalRequestPageResult page(Long tenantId, ApprovalRequestPageCriteria criteria);

    ApprovalRequestRecord approve(Long tenantId, Long reviewerId, String requestId, Long approvalRequestId);

    ApprovalRequestRecord reject(Long tenantId, Long reviewerId, String requestId, Long approvalRequestId);
}
