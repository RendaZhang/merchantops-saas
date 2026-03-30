package com.renda.merchantops.domain.approval;

public interface ApprovalTicketCommentProposalPort {

    PreparedTicketCommentApproval prepareProposal(Long tenantId, TicketCommentApprovalCommand command);
}
