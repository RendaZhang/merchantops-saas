package com.renda.merchantops.api.dto.ticket.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "Tenant-scoped ticket page result")
public class TicketPageResponse {

    @Schema(description = "Current page items")
    private List<TicketListItemResponse> items;

    @Schema(description = "Current page index, zero-based", example = "0")
    private int page;

    @Schema(description = "Current page size", example = "10")
    private int size;

    @Schema(description = "Total item count", example = "2")
    private long total;

    @Schema(description = "Total page count", example = "1")
    private int totalPages;
}
