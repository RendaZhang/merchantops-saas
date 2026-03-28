package com.renda.merchantops.domain.importjob;

import java.util.List;

public record ImportJobAiInteractionPageResult(
        List<ImportJobAiInteractionItem> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}
