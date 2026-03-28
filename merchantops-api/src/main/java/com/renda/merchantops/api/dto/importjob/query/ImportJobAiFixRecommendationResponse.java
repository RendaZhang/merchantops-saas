package com.renda.merchantops.api.dto.importjob.query;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "AI-generated import job fix recommendation")
public record ImportJobAiFixRecommendationResponse(
        @Schema(description = "Import job ID", example = "1201")
        Long importJobId,
        @Schema(description = "AI-generated suggestion-only summary grounded in the sanitized import failure context", example = "The job is primarily blocked by tenant role validation plus a smaller duplicate-username tail, so operators should separate role remediation from uniqueness cleanup before any replay preparation.")
        String summary,
        @Schema(description = "Recommended fixes for grounded row-level import error codes")
        List<RecommendedFix> recommendedFixes,
        @Schema(description = "AI confidence notes about ambiguity, missing signal, or review risk")
        List<String> confidenceNotes,
        @Schema(description = "Suggested operator checks before any manual edit or replay preparation")
        List<String> recommendedOperatorChecks,
        @Schema(description = "Prompt version used for generation", example = "import-fix-recommendation-v1")
        String promptVersion,
        @Schema(description = "Resolved model identifier", example = "gpt-4.1-mini")
        String modelId,
        @Schema(description = "Generation timestamp", example = "2026-03-28T10:40:15")
        LocalDateTime generatedAt,
        @Schema(description = "End-to-end provider latency in milliseconds", example = "566")
        Long latencyMs,
        @Schema(description = "Request trace identifier", example = "import-ai-fix-recommendation-req-1")
        String requestId
) {
    @Schema(description = "Suggested fix for one grounded row-level import error code")
    public record RecommendedFix(
            @Schema(description = "Grounded row-level import error code from the current import job", example = "UNKNOWN_ROLE")
            String errorCode,
            @Schema(description = "Short generic operator action that does not expose replacement values", example = "Verify that the referenced role codes exist in the current tenant before preparing replay input.")
            String recommendedAction,
            @Schema(description = "Why this fix is suggested, grounded in sanitized import failure context", example = "The sampled failures all point to tenant role validation rather than CSV shape corruption.")
            String reasoning,
            @Schema(description = "Whether an operator should manually review this recommendation before reuse", example = "true")
            boolean reviewRequired,
            @Schema(description = "Estimated affected row count derived from local errorCodeCounts", example = "7")
            Long affectedRowsEstimate
    ) {
    }
}
