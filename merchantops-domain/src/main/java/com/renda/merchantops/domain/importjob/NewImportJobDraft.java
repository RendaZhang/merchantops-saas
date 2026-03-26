package com.renda.merchantops.domain.importjob;

public record NewImportJobDraft(
        String importType,
        String sourceType,
        String sourceFilename,
        String storageKey,
        Long sourceJobId
) {
}
