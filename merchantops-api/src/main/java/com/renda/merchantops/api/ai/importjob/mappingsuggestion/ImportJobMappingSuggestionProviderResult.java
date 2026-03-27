package com.renda.merchantops.api.ai.importjob.mappingsuggestion;

import com.renda.merchantops.api.ai.core.AiUsageAwareResult;

import java.util.List;

public record ImportJobMappingSuggestionProviderResult(
        String summary,
        List<SuggestedFieldMapping> suggestedFieldMappings,
        List<String> confidenceNotes,
        List<String> recommendedOperatorChecks,
        String modelId,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        Long costMicros
) implements AiUsageAwareResult {

    public record SuggestedFieldMapping(
            String canonicalField,
            ObservedColumnSignal observedColumnSignal,
            String reasoning,
            boolean reviewRequired
    ) {
    }

    public record ObservedColumnSignal(
            String headerName,
            Integer headerPosition
    ) {
    }
}
