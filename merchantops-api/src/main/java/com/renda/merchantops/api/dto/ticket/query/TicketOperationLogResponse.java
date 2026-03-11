package com.renda.merchantops.api.dto.ticket.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(description = "Workflow-level ticket operation log entry")
public class TicketOperationLogResponse {

    @Schema(description = "Operation log ID", example = "21")
    private Long id;

    @Schema(description = "Operation type", example = "ASSIGNED")
    private String operationType;

    @Schema(description = "Human-readable operation detail", example = "assigned to ops")
    private String detail;

    @Schema(description = "Operator user ID", example = "1")
    private Long operatorId;

    @Schema(description = "Operator username", example = "admin")
    private String operatorUsername;

    @Schema(description = "Created time", example = "2026-03-11T10:10:00")
    private LocalDateTime createdAt;
}
