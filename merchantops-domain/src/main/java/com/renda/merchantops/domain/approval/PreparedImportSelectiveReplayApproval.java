package com.renda.merchantops.domain.approval;

import java.util.List;

public record PreparedImportSelectiveReplayApproval(
        Long sourceJobId,
        List<String> errorCodes,
        Long sourceInteractionId,
        String proposalReason,
        String payloadJson
) {
    public PreparedImportSelectiveReplayApproval {
        errorCodes = List.copyOf(errorCodes);
    }
}
