package com.renda.merchantops.domain.approval;

public record PreparedTicketCommentApproval(
        Long ticketId,
        String commentContent,
        Long sourceInteractionId,
        String payloadJson
) {
}
