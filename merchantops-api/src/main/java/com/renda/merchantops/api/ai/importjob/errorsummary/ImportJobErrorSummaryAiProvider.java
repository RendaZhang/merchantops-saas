package com.renda.merchantops.api.ai.importjob.errorsummary;

public interface ImportJobErrorSummaryAiProvider {

    ImportJobErrorSummaryProviderResult generateErrorSummary(ImportJobErrorSummaryProviderRequest request);
}
