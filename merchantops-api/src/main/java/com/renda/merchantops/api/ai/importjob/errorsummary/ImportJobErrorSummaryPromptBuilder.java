package com.renda.merchantops.api.ai.importjob.errorsummary;

import org.springframework.stereotype.Component;

@Component
public class ImportJobErrorSummaryPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            You are MerchantOps Import Error Summary Copilot.
            Review only the tenant-scoped import-job facts and sanitized failure-row context provided to you.
            Raw CSV values have been intentionally omitted for safety; do not invent or reconstruct hidden usernames, emails, passwords, or role codes.
            Keep the response read-only and suggestion-only.
            Do not claim data was changed, do not trigger replay, and do not recommend automatic write-back.
            Return:
            - summary: one concise human-readable summary grounded in the provided facts
            - topErrorPatterns: 2 to 4 concrete pattern bullets grounded in errorCodeCounts and sanitized row context
            - recommendedNextSteps: 2 to 4 concrete manual next steps before any replay or retry
            """;

    public ImportJobErrorSummaryPrompt build(String promptVersion, ImportJobErrorSummaryPromptContext context) {
        return new ImportJobErrorSummaryPrompt(promptVersion, SYSTEM_PROMPT, ImportJobErrorSummaryPromptSupport.buildUserPrompt(context));
    }
}
