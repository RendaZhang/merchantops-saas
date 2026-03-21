package com.renda.merchantops.api.dto.ticket.query;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "AI-generated ticket triage suggestion")
public record TicketAiTriageResponse(
        @Schema(description = "Ticket ID", example = "302")
        Long ticketId,
        @Schema(description = "Suggested short classification label", example = "DEVICE_ISSUE")
        String classification,
        @Schema(description = "Suggested priority", allowableValues = {"LOW", "MEDIUM", "HIGH"}, example = "HIGH")
        TicketAiTriagePriority priority,
        @Schema(description = "Short human-readable reasoning grounded in the current ticket context", example = "The ticket describes a store-facing printer outage during peak hours with no confirmed fix yet, so it should be treated as a high-priority device issue.")
        String reasoning,
        @Schema(description = "Prompt version used for generation", example = "ticket-triage-v1")
        String promptVersion,
        @Schema(description = "Resolved model identifier", example = "gpt-4.1-mini")
        String modelId,
        @Schema(description = "Generation timestamp", example = "2026-03-21T14:20:15")
        LocalDateTime generatedAt,
        @Schema(description = "End-to-end provider latency in milliseconds", example = "418")
        Long latencyMs,
        @Schema(description = "Request trace identifier", example = "ticket-ai-triage-req-1")
        String requestId
) {
}
