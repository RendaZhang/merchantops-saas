package com.renda.merchantops.domain.importjob;

import java.util.List;

public record ImportJobErrorPageResult(
        List<ImportJobErrorRecord> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}
