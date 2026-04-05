package com.renda.merchantops.api.dto.ai.query;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Tenant-scoped AI usage and cost summary")
public record AiInteractionUsageSummaryResponse(
        @Schema(description = "Inclusive start time used for the summary when provided", example = "2026-04-01T00:00:00")
        LocalDateTime from,
        @Schema(description = "Inclusive end time used for the summary when provided", example = "2026-04-05T23:59:59")
        LocalDateTime to,
        @Schema(description = "Total matched AI interactions", example = "6")
        Long totalInteractions,
        @Schema(description = "Matched interactions with status SUCCEEDED", example = "4")
        Long succeededCount,
        @Schema(description = "Matched interactions with non-SUCCEEDED statuses", example = "2")
        Long failedCount,
        @Schema(description = "Summed prompt tokens across matched rows", example = "520")
        Long totalPromptTokens,
        @Schema(description = "Summed completion tokens across matched rows", example = "243")
        Long totalCompletionTokens,
        @Schema(description = "Summed total tokens across matched rows", example = "763")
        Long totalTokens,
        @Schema(description = "Summed runtime cost in raw micros across matched rows", example = "8200")
        Long totalCostMicros,
        @Schema(description = "Breakdown by interaction type")
        List<ByInteractionType> byInteractionType,
        @Schema(description = "Breakdown by interaction status")
        List<ByStatus> byStatus
) {

    @Schema(description = "Usage breakdown by interaction type")
    public record ByInteractionType(
            @Schema(description = "Stored interaction type", example = "SUMMARY")
            String interactionType,
            @Schema(description = "Matched row count", example = "2")
            Long count,
            @Schema(description = "Matched row count with status SUCCEEDED", example = "2")
            Long succeededCount,
            @Schema(description = "Matched row count with non-SUCCEEDED statuses", example = "0")
            Long failedCount,
            @Schema(description = "Summed total tokens across matched rows", example = "303")
            Long totalTokens,
            @Schema(description = "Summed runtime cost in raw micros across matched rows", example = "3100")
            Long totalCostMicros
    ) {
    }

    @Schema(description = "Usage breakdown by status")
    public record ByStatus(
            @Schema(description = "Stored interaction status", example = "SUCCEEDED")
            String status,
            @Schema(description = "Matched row count", example = "4")
            Long count,
            @Schema(description = "Summed total tokens across matched rows", example = "763")
            Long totalTokens,
            @Schema(description = "Summed runtime cost in raw micros across matched rows", example = "8200")
            Long totalCostMicros
    ) {
    }
}
