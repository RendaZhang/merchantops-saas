package com.renda.merchantops.api.dto.importjob.query;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "AI-generated import job error summary suggestion")
public record ImportJobAiErrorSummaryResponse(
        @Schema(description = "Import job ID", example = "1201")
        Long importJobId,
        @Schema(description = "AI-generated suggestion-only summary grounded in the sanitized import context", example = "The job is dominated by tenant role validation failures plus a smaller set of duplicate usernames. Most failing rows are structurally complete, so the next human step is to correct role mappings and decide which duplicate usernames should be changed before replay.")
        String summary,
        @Schema(description = "Top error patterns observed in the sanitized prompt window")
        List<String> topErrorPatterns,
        @Schema(description = "Suggested next manual steps before any replay or retry")
        List<String> recommendedNextSteps,
        @Schema(description = "Prompt version used for generation", example = "import-error-summary-v1")
        String promptVersion,
        @Schema(description = "Resolved model identifier", example = "gpt-4.1-mini")
        String modelId,
        @Schema(description = "Generation timestamp", example = "2026-03-27T10:20:15")
        LocalDateTime generatedAt,
        @Schema(description = "End-to-end provider latency in milliseconds", example = "512")
        Long latencyMs,
        @Schema(description = "Request trace identifier", example = "import-ai-error-summary-req-1")
        String requestId
) {
}
