package com.renda.merchantops.api.dto.importjob.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Import job error page query")
public class ImportJobErrorPageQuery {

    @Schema(description = "Zero-based page index", example = "0", defaultValue = "0")
    private Integer page = 0;

    @Schema(description = "Page size", example = "10", defaultValue = "10")
    private Integer size = 10;

    @Schema(description = "Exact import error code filter", example = "UNKNOWN_ROLE")
    private String errorCode;
}
