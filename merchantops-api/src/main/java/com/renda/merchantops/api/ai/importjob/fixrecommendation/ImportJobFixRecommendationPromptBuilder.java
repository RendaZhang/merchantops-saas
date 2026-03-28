package com.renda.merchantops.api.ai.importjob.fixrecommendation;

import org.springframework.stereotype.Component;

@Component
public class ImportJobFixRecommendationPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            You are MerchantOps Import Fix Recommendation Copilot.
            Review only the tenant-scoped import-job facts and grounded row-level sanitized failure groups provided to you.
            Raw CSV values have been intentionally omitted for safety; do not invent or reconstruct hidden usernames, display names, emails, passwords, role codes, or any other row values.
            This slice is read-only and suggestion-only.
            Do not generate replacement values, do not quote source row content, do not write scripts, do not describe automatic execution, and do not claim data was changed.
            Only recommend fixes for the grounded row-level error codes included in the prompt.
            Return:
            - summary: one concise human-readable summary grounded in the provided facts
            - recommendedFixes: 1 to 5 items; for each item return errorCode, recommendedAction, reasoning, reviewRequired
            - confidenceNotes: 1 to 4 notes about ambiguity, missing signal, or review risk
            - recommendedOperatorChecks: 2 to 4 concrete manual checks before any downstream replay preparation
            Keep recommendedAction generic and operator-facing. It must describe what to verify or correct, not the hidden replacement value itself.
            """;

    public ImportJobFixRecommendationPrompt build(String promptVersion, ImportJobFixRecommendationPromptContext context) {
        return new ImportJobFixRecommendationPrompt(
                promptVersion,
                SYSTEM_PROMPT,
                ImportJobFixRecommendationPromptSupport.buildUserPrompt(context)
        );
    }
}
