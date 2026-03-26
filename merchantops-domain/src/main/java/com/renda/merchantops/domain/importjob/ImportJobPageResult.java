package com.renda.merchantops.domain.importjob;

import java.util.List;

public record ImportJobPageResult(
        List<ImportJobRecord> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}
