package com.renda.merchantops.api.dto.ticket.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(description = "One AI interaction record for a current-tenant ticket")
public class TicketAiInteractionListItemResponse {

    @Schema(description = "AI interaction record id", example = "9003")
    private Long id;

    @Schema(description = "Exact interaction type", example = "SUMMARY")
    private String interactionType;

    @Schema(description = "Exact interaction status", example = "SUCCEEDED")
    private String status;

    @Schema(description = "Operator-visible stored output summary when available", example = "Issue: Printer cable replacement is in progress under ops. Current: the ticket is assigned and the latest signal says cable swap started. Next: confirm the replacement outcome and close the ticket if the printer is healthy.")
    private String outputSummary;

    @Schema(description = "Prompt version captured for the interaction", example = "ticket-summary-v1")
    private String promptVersion;

    @Schema(description = "Resolved model id captured for the interaction when available", example = "gpt-4.1-mini")
    private String modelId;

    @Schema(description = "Measured latency in milliseconds", example = "412")
    private Long latencyMs;

    @Schema(description = "Request id linked to the interaction", example = "ticket-ai-summary-req-1")
    private String requestId;

    @Schema(description = "Record creation time", example = "2026-03-22T09:00:00")
    private LocalDateTime createdAt;
}
