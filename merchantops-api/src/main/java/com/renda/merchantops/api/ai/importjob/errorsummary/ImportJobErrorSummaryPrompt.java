package com.renda.merchantops.api.ai.importjob.errorsummary;

public record ImportJobErrorSummaryPrompt(
        String promptVersion,
        String systemPrompt,
        String userPrompt
) {
}
