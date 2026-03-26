package com.renda.merchantops.domain.approval;

public interface ApprovalActionPort {

    void disableUser(Long tenantId, Long reviewerId, String requestId, Long userId);
}
