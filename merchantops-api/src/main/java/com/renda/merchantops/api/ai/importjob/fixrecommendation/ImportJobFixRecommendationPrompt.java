package com.renda.merchantops.api.ai.importjob.fixrecommendation;

public record ImportJobFixRecommendationPrompt(
        String promptVersion,
        String systemPrompt,
        String userPrompt
) {
}
