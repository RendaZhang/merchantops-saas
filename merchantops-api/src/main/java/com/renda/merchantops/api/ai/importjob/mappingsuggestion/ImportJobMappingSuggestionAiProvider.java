package com.renda.merchantops.api.ai.importjob.mappingsuggestion;

public interface ImportJobMappingSuggestionAiProvider {

    ImportJobMappingSuggestionProviderResult generateMappingSuggestion(ImportJobMappingSuggestionProviderRequest request);
}
