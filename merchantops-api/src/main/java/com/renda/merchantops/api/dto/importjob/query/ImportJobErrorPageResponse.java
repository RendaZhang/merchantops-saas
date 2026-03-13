package com.renda.merchantops.api.dto.importjob.query;

import java.util.List;

public record ImportJobErrorPageResponse(
        List<ImportJobErrorItemResponse> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}
