package com.renda.merchantops.api.dto.importjob.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Import job AI interaction history page query within current tenant")
public class ImportJobAiInteractionPageQuery {

    @Schema(description = "Zero-based page index", example = "0", defaultValue = "0")
    private Integer page = 0;

    @Schema(description = "Page size", example = "10", defaultValue = "10")
    private Integer size = 10;

    @Schema(description = "Exact interaction type filter", example = "ERROR_SUMMARY")
    private String interactionType;

    @Schema(description = "Exact interaction status filter", example = "SUCCEEDED")
    private String status;
}
