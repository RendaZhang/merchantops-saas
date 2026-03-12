package com.renda.merchantops.api.dto.importjob.query;

import java.util.List;

public record ImportJobPageResponse(
        List<ImportJobListItemResponse> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}
