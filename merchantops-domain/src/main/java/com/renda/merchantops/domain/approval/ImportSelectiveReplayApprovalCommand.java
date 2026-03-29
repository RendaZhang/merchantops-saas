package com.renda.merchantops.domain.approval;

import java.util.List;

public record ImportSelectiveReplayApprovalCommand(
        Long sourceJobId,
        List<String> errorCodes,
        Long sourceInteractionId,
        String proposalReason
) {
    public ImportSelectiveReplayApprovalCommand {
        errorCodes = errorCodes == null ? null : List.copyOf(errorCodes);
    }
}
