package com.renda.merchantops.api.dto.importjob.command;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Selective failed-row replay proposal request")
public class ImportJobSelectiveReplayProposalRequest {

    @NotEmpty(message = "errorCodes must not be empty")
    @ArraySchema(schema = @Schema(description = "Exact import error code proposed for selective replay", example = "UNKNOWN_ROLE"))
    private List<@NotBlank(message = "errorCodes must not contain blank values") String> errorCodes;

    @Positive(message = "sourceInteractionId must be greater than 0")
    @Schema(description = "Optional succeeded FIX_RECOMMENDATION interaction id used as proposal provenance", example = "9103")
    private Long sourceInteractionId;

    @Size(max = 255, message = "proposalReason must be at most 255 characters")
    @Schema(description = "Optional short operator-authored reason for this proposal", example = "Approve replay only for role-related failures after tenant role catalog review.")
    private String proposalReason;
}
