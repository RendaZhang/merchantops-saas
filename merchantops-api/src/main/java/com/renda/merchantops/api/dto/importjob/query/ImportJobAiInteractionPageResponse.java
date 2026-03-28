package com.renda.merchantops.api.dto.importjob.query;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Tenant-scoped import job AI interaction history page result")
public record ImportJobAiInteractionPageResponse(
        @Schema(description = "Current page items")
        List<ImportJobAiInteractionListItemResponse> items,
        @Schema(description = "Current page index, zero-based", example = "0")
        int page,
        @Schema(description = "Current page size", example = "10")
        int size,
        @Schema(description = "Total item count", example = "3")
        long total,
        @Schema(description = "Total page count", example = "1")
        int totalPages
) {
}
