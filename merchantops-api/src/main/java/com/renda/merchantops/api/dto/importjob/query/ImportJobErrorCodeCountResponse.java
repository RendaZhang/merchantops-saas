package com.renda.merchantops.api.dto.importjob.query;

public record ImportJobErrorCodeCountResponse(
        String errorCode,
        long count
) {
}
