package com.renda.merchantops.api.approval;

import java.util.List;

record ImportSelectiveReplayApprovalPayload(
        Long sourceJobId,
        List<String> errorCodes,
        Long sourceInteractionId,
        String proposalReason
) {
}
