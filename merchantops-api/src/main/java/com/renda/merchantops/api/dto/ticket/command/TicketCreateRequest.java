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
@Schema(description = "Create ticket request within current tenant")
public class TicketCreateRequest {

    @NotBlank(message = "title must not be blank")
    @Size(max = 128, message = "title length must be less than or equal to 128")
    @Schema(description = "Ticket title", example = "POS printer offline")
    private String title;

    @Size(max = 2000, message = "description length must be less than or equal to 2000")
    @Schema(description = "Ticket description", example = "Store POS printer stopped responding during lunch peak.")
    private String description;
}
