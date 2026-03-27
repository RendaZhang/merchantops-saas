package com.renda.merchantops.api.ai.importjob.mappingsuggestion;

public record ImportJobMappingSuggestionPrompt(
        String promptVersion,
        String systemPrompt,
        String userPrompt
) {
}
