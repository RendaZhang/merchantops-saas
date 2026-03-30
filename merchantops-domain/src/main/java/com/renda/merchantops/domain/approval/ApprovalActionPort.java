package com.renda.merchantops.domain.approval;

public interface ApprovalActionPort {

    void disableUser(Long tenantId, Long reviewerId, String requestId, Long userId);

    void replayImportJobSelective(Long tenantId, Long reviewerId, String requestId, Long sourceJobId, String payloadJson);

    void createTicketComment(Long tenantId, Long reviewerId, String requestId, Long ticketId, String payloadJson);
}
