package com.renda.merchantops.api.dto.ticket.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ticket status update request")
public class TicketStatusUpdateRequest {

    @NotBlank(message = "status must not be blank")
    @Pattern(regexp = "OPEN|IN_PROGRESS|CLOSED", message = "status must be one of OPEN, IN_PROGRESS, CLOSED")
    @Schema(description = "Target ticket status (supports reopen via CLOSED -> OPEN)", example = "IN_PROGRESS")
    private String status;
}
