package com.renda.merchantops.api.dto.importjob.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Import job page query")
public class ImportJobPageQuery {

    @Schema(description = "Zero-based page index", example = "0", defaultValue = "0")
    private Integer page = 0;

    @Schema(description = "Page size", example = "10", defaultValue = "10")
    private Integer size = 10;
}
