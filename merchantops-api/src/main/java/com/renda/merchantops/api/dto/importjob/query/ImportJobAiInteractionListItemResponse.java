package com.renda.merchantops.api.dto.importjob.query;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "One AI interaction record for a current-tenant import job")
public record ImportJobAiInteractionListItemResponse(
        @Schema(description = "AI interaction record id", example = "9103")
        Long id,
        @Schema(description = "Exact interaction type", example = "FIX_RECOMMENDATION")
        String interactionType,
        @Schema(description = "Exact interaction status", example = "SUCCEEDED")
        String status,
        @Schema(description = "Operator-visible stored output summary when available", example = "The job is mostly blocked by tenant role validation, with a smaller duplicate-username tail that should be handled separately before replay.")
        String outputSummary,
        @Schema(description = "Prompt version captured for the interaction", example = "import-fix-recommendation-v1")
        String promptVersion,
        @Schema(description = "Resolved model id captured for the interaction when available", example = "gpt-4.1-mini")
        String modelId,
        @Schema(description = "Measured latency in milliseconds", example = "566")
        Long latencyMs,
        @Schema(description = "Request id linked to the interaction", example = "import-ai-fix-recommendation-req-1")
        String requestId,
        @Schema(description = "Provider-reported prompt token count when available for operator-visible runtime metadata", example = "145")
        Integer usagePromptTokens,
        @Schema(description = "Provider-reported completion token count when available for operator-visible runtime metadata", example = "87")
        Integer usageCompletionTokens,
        @Schema(description = "Provider-reported total token count when available for operator-visible runtime metadata", example = "232")
        Integer usageTotalTokens,
        @Schema(description = "Provider-reported runtime cost in raw micros when available; null for failed or unmetered records", example = "2200")
        Long usageCostMicros,
        @Schema(description = "Record creation time", example = "2026-03-28T10:40:15")
        LocalDateTime createdAt
) {
}
