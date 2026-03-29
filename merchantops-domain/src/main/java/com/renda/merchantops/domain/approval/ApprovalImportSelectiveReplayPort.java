package com.renda.merchantops.domain.approval;

public interface ApprovalImportSelectiveReplayPort {

    PreparedImportSelectiveReplayApproval prepareProposal(Long tenantId, ImportSelectiveReplayApprovalCommand command);
}
