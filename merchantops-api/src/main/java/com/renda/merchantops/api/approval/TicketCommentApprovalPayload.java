package com.renda.merchantops.api.approval;

public record TicketCommentApprovalPayload(
        String commentContent,
        Long sourceInteractionId
) {
}
