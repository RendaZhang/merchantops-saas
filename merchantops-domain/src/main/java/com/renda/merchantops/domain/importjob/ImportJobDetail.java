package com.renda.merchantops.domain.importjob;

import java.util.List;

public record ImportJobDetail(
        ImportJobRecord job,
        List<ImportJobErrorCount> errorCodeCounts,
        List<ImportJobErrorRecord> itemErrors
) {
}
