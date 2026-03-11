package com.renda.merchantops.api.dto.ticket.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(description = "Ticket list item in current tenant")
public class TicketListItemResponse {

    @Schema(description = "Ticket ID", example = "1")
    private Long id;

    @Schema(description = "Ticket title", example = "POS printer offline")
    private String title;

    @Schema(description = "Ticket status", example = "OPEN")
    private String status;

    @Schema(description = "Current assignee user ID", example = "2")
    private Long assigneeId;

    @Schema(description = "Current assignee username", example = "ops")
    private String assigneeUsername;

    @Schema(description = "Created time", example = "2026-03-11T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated time", example = "2026-03-11T10:20:00")
    private LocalDateTime updatedAt;
}
