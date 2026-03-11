package com.renda.merchantops.api.dto.ticket.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ticket assignee replacement request")
public class TicketAssigneeUpdateRequest {

    @NotNull(message = "assigneeId must not be null")
    @Schema(description = "Assignee user ID from the current tenant", example = "2")
    private Long assigneeId;
}
