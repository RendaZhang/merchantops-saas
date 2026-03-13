package com.renda.merchantops.api.dto.approval.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(description = "Tenant-scoped approval request list item")
public class ApprovalRequestListItemResponse {

    @Schema(description = "Approval request ID", example = "9001")
    private Long id;

    @Schema(description = "Action type", example = "USER_STATUS_DISABLE")
    private String actionType;

    @Schema(description = "Entity type", example = "USER")
    private String entityType;

    @Schema(description = "Entity ID", example = "103")
    private Long entityId;

    @Schema(description = "Requester user ID", example = "101")
    private Long requestedBy;

    @Schema(description = "Reviewer user ID", example = "102")
    private Long reviewedBy;

    @Schema(description = "Current approval status", example = "PENDING")
    private String status;

    @Schema(description = "Created time")
    private LocalDateTime createdAt;

    @Schema(description = "Reviewed time")
    private LocalDateTime reviewedAt;

    @Schema(description = "Executed time")
    private LocalDateTime executedAt;
}
