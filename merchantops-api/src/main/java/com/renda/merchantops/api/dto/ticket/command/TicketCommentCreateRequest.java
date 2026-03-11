package com.renda.merchantops.api.dto.ticket.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create ticket comment request")
public class TicketCommentCreateRequest {

    @NotBlank(message = "content must not be blank")
    @Size(max = 2000, message = "content length must be less than or equal to 2000")
    @Schema(description = "Comment content", example = "Checked the store network. Restarting the printer now.")
    private String content;
}
