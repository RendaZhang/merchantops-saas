package com.renda.merchantops.api.dto.approval.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Approval request page query within current tenant")
public class ApprovalRequestPageQuery {

    @Schema(description = "Zero-based page index", example = "0", defaultValue = "0")
    private Integer page = 0;

    @Schema(description = "Page size", example = "10", defaultValue = "10")
    private Integer size = 10;

    @Schema(description = "Exact status filter", example = "PENDING")
    private String status;

    @Schema(description = "Exact action type filter", example = "USER_STATUS_DISABLE")
    private String actionType;

    @Schema(description = "Exact requester user id filter", example = "101")
    private Long requestedBy;
}
