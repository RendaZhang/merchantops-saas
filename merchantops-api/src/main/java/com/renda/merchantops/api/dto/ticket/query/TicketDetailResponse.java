package com.renda.merchantops.api.dto.ticket.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "Tenant-scoped ticket detail")
public class TicketDetailResponse {

    @Schema(description = "Ticket ID", example = "1")
    private Long id;

    @Schema(description = "Tenant ID", example = "1")
    private Long tenantId;

    @Schema(description = "Ticket title", example = "POS printer offline")
    private String title;

    @Schema(description = "Ticket description", example = "Store POS printer stopped responding during lunch peak.")
    private String description;

    @Schema(description = "Ticket status", example = "IN_PROGRESS")
    private String status;

    @Schema(description = "Current assignee user ID", example = "2")
    private Long assigneeId;

    @Schema(description = "Current assignee username", example = "ops")
    private String assigneeUsername;

    @Schema(description = "Creator user ID", example = "1")
    private Long createdBy;

    @Schema(description = "Creator username", example = "admin")
    private String createdByUsername;

    @Schema(description = "Created time", example = "2026-03-11T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated time", example = "2026-03-11T10:20:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Ticket comments in ascending order")
    private List<TicketCommentResponse> comments;

    @Schema(description = "Workflow-level operation logs in ascending order")
    private List<TicketOperationLogResponse> operationLogs;
}
