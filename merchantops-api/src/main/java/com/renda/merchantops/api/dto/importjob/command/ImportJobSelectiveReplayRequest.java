package com.renda.merchantops.api.dto.importjob.command;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Selective failed-row replay request")
public class ImportJobSelectiveReplayRequest {

    @NotEmpty(message = "errorCodes must not be empty")
    @ArraySchema(schema = @Schema(description = "Exact import error code to replay", example = "UNKNOWN_ROLE"))
    private List<@NotBlank(message = "errorCodes must not contain blank values") String> errorCodes;
}
