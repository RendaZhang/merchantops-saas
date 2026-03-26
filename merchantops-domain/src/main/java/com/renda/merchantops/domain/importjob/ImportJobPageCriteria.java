package com.renda.merchantops.domain.importjob;

public record ImportJobPageCriteria(
        int page,
        int size,
        String status,
        String importType,
        Long requestedBy,
        boolean hasFailuresOnly
) {
}
