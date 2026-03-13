package com.renda.merchantops.api.dto.ticket.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ticket page query within current tenant")
public class TicketPageQuery {

    @Schema(description = "Zero-based page index", example = "0", defaultValue = "0")
    private Integer page = 0;

    @Schema(description = "Page size", example = "10", defaultValue = "10")
    private Integer size = 10;

    @Schema(description = "Exact ticket status filter", example = "OPEN")
    private String status;

    @Schema(description = "Exact assignee user ID filter in current tenant", example = "102")
    private Long assigneeId;

    @Schema(description = "Keyword search against title and description", example = "printer")
    private String keyword;

    @Schema(description = "When true, only return unassigned tickets (assigneeId is null)", example = "true", defaultValue = "false")
    private Boolean unassignedOnly;
}
