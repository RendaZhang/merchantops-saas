package com.renda.merchantops.api.dto.importjob.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Import job create request")
public class ImportJobCreateRequest {

    @NotBlank
    @Schema(description = "Import type", example = "USER_CSV")
    private String importType;
}
