package com.renda.merchantops.domain.approval;

public record TicketCommentApprovalCommand(
        Long ticketId,
        String commentContent,
        Long sourceInteractionId
) {
}
