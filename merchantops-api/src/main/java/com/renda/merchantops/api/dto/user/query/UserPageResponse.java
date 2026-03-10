package com.renda.merchantops.api.dto.user.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "Tenant-scoped user page result")
public class UserPageResponse {

    @Schema(description = "Current page items")
    private List<UserListItemResponse> items;

    @Schema(description = "Current page index, zero-based", example = "0")
    private int page;

    @Schema(description = "Current page size", example = "20")
    private int size;

    @Schema(description = "Total item count", example = "3")
    private long total;

    @Schema(description = "Total page count", example = "1")
    private int totalPages;
}
