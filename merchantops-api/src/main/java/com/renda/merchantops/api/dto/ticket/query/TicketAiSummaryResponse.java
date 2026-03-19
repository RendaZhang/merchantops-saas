package com.renda.merchantops.api.dto.ticket.query;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "AI-generated ticket summary suggestion")
public record TicketAiSummaryResponse(
        @Schema(description = "Ticket ID", example = "302")
        Long ticketId,
        @Schema(description = "AI-generated suggestion-only summary", example = "Issue: Printer cable replacement is in progress under ops. Current: the ticket is assigned and the latest signal says cable swap started. Next: confirm the replacement outcome and close the ticket if the printer is healthy.")
        String summary,
        @Schema(description = "Prompt version used for generation", example = "ticket-summary-v1")
        String promptVersion,
        @Schema(description = "Resolved model identifier", example = "gpt-4.1-mini")
        String modelId,
        @Schema(description = "Generation timestamp", example = "2026-03-19T13:20:15")
        LocalDateTime generatedAt,
        @Schema(description = "End-to-end provider latency in milliseconds", example = "412")
        Long latencyMs,
        @Schema(description = "Request trace identifier", example = "ticket-ai-summary-req-1")
        String requestId
) {
}
