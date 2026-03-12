package com.renda.merchantops.api.dto.importjob.command;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Edited failed-row replay request")
public class ImportJobEditedReplayRequest {

    @NotEmpty(message = "items must not be empty")
    @Valid
    @ArraySchema(schema = @Schema(implementation = ImportJobEditedReplayItemRequest.class))
    private List<@NotNull(message = "items must not contain null entries")
            @Valid ImportJobEditedReplayItemRequest> items;
}
