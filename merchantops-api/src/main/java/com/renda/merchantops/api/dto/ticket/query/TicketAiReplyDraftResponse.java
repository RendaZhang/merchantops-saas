package com.renda.merchantops.api.dto.ticket.query;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "AI-generated internal ticket reply draft suggestion")
public record TicketAiReplyDraftResponse(
        @Schema(description = "Ticket ID", example = "302")
        Long ticketId,
        @Schema(description = "Server-assembled comment-ready internal draft text", example = "Quick update from ops.\n\nThe ticket is currently in progress and the latest comment confirms the cable swap has started.\n\nNext step: Confirm whether the replacement restored printer health and note any blocker before closing.\n\nI will update the ticket again once the verification result is confirmed.")
        String draftText,
        @Schema(description = "Opening section of the draft", example = "Quick update from ops.")
        String opening,
        @Schema(description = "Main body section of the draft", example = "The ticket is currently in progress and the latest comment confirms the cable swap has started.")
        String body,
        @Schema(description = "Next-step section of the draft without the server-added label", example = "Confirm whether the replacement restored printer health and note any blocker before closing.")
        String nextStep,
        @Schema(description = "Closing section of the draft", example = "I will update the ticket again once the verification result is confirmed.")
        String closing,
        @Schema(description = "Prompt version used for generation", example = "ticket-reply-draft-v1")
        String promptVersion,
        @Schema(description = "Resolved model identifier", example = "gpt-4.1-mini")
        String modelId,
        @Schema(description = "Generation timestamp", example = "2026-03-21T15:10:15")
        LocalDateTime generatedAt,
        @Schema(description = "End-to-end provider latency in milliseconds", example = "436")
        Long latencyMs,
        @Schema(description = "Request trace identifier", example = "ticket-ai-reply-draft-req-1")
        String requestId
) {
}
