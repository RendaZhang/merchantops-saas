package com.renda.merchantops.domain.importjob;

public record ImportJobErrorPageCriteria(
        int page,
        int size,
        String errorCode
) {
}
