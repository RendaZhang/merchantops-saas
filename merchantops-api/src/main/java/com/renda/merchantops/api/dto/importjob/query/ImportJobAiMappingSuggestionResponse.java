package com.renda.merchantops.api.dto.importjob.query;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "AI-generated import job mapping suggestion")
public record ImportJobAiMappingSuggestionResponse(
        @Schema(description = "Import job ID", example = "1201")
        Long importJobId,
        @Schema(description = "AI-generated suggestion-only summary grounded in the sanitized import context", example = "The failed header still looks close to the canonical USER_CSV schema, so the safest next step is to confirm the proposed field mappings before preparing any replay input.")
        String summary,
        @Schema(description = "Canonical USER_CSV field mappings suggested from sanitized header and failure signals")
        List<SuggestedFieldMapping> suggestedFieldMappings,
        @Schema(description = "AI confidence notes about ambiguity, missing signal, or review risk")
        List<String> confidenceNotes,
        @Schema(description = "Suggested operator checks before any manual edit or replay preparation")
        List<String> recommendedOperatorChecks,
        @Schema(description = "Prompt version used for generation", example = "import-mapping-suggestion-v1")
        String promptVersion,
        @Schema(description = "Resolved model identifier", example = "gpt-4.1-mini")
        String modelId,
        @Schema(description = "Generation timestamp", example = "2026-03-27T10:30:15")
        LocalDateTime generatedAt,
        @Schema(description = "End-to-end provider latency in milliseconds", example = "544")
        Long latencyMs,
        @Schema(description = "Request trace identifier", example = "import-ai-mapping-suggestion-req-1")
        String requestId
) {
    @Schema(description = "Suggested mapping for one canonical USER_CSV field")
    public record SuggestedFieldMapping(
            @Schema(description = "Canonical USER_CSV field name", example = "username")
            String canonicalField,
            @Schema(description = "Observed source-column signal suggested for the canonical field; null when the model cannot make a safe single-column match")
            ObservedColumnSignal observedColumnSignal,
            @Schema(description = "Why this mapping is suggested or why manual review is still required", example = "`login` is the closest observed header for the tenant username field.")
            String reasoning,
            @Schema(description = "Whether an operator should manually review this mapping before using it downstream", example = "false")
            boolean reviewRequired
    ) {
    }

    @Schema(description = "Observed source-column signal extracted from sanitized header context")
    public record ObservedColumnSignal(
            @Schema(description = "Normalized header token", example = "login")
            String headerName,
            @Schema(description = "1-based position within the observed header row", example = "1")
            Integer headerPosition
    ) {
    }
}
