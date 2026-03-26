package com.renda.merchantops.domain.importjob;

public record ImportJobErrorCount(
        String errorCode,
        long count
) {
}
