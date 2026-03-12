package com.renda.merchantops.api.dto.importjob.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Import job page query")
public class ImportJobPageQuery {

    @Schema(description = "Zero-based page index", example = "0", defaultValue = "0")
    private Integer page = 0;

    @Schema(description = "Page size", example = "10", defaultValue = "10")
    private Integer size = 10;

    @Schema(description = "Exact import job status filter", example = "FAILED")
    private String status;

    @Schema(description = "Exact import type filter", example = "USER_CSV")
    private String importType;

    @Schema(description = "Exact requester user ID filter in current tenant", example = "101")
    private Long requestedBy;

    @Schema(description = "When true, return only jobs with failureCount > 0", example = "true", defaultValue = "false")
    private Boolean hasFailuresOnly = false;
}
