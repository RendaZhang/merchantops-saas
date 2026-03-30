package com.renda.merchantops.api.dto.ticket.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create approval-backed ticket comment proposal request")
public class TicketCommentProposalRequest {

    @NotBlank(message = "commentContent must not be blank")
    @Size(max = 2000, message = "commentContent length must be less than or equal to 2000")
    @Schema(description = "Final proposed ticket comment content", example = "Quick update from ops. The cable swap is still in progress and I will confirm the verification result next.")
    private String commentContent;

    @Positive(message = "sourceInteractionId must be greater than 0")
    @Schema(description = "Optional succeeded REPLY_DRAFT interaction id used as proposal provenance", example = "9002")
    private Long sourceInteractionId;
}
