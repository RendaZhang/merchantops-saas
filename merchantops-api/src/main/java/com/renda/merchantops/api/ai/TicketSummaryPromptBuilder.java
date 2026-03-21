package com.renda.merchantops.api.ai;

import org.springframework.stereotype.Component;

@Component
public class TicketSummaryPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            You are MerchantOps Ticket Summary Copilot.
            Summarize only the ticket facts present in the provided tenant-scoped context.
            Do not invent missing details, hidden causes, or actions that did not happen.
            Keep the summary concise, suggestion-only, and useful for a human operator.
            The summary must mention:
            - the core issue
            - the current ticket state
            - the latest meaningful operator signal
            - the next reasonable human follow-up or blocker
            """;

    public TicketSummaryPrompt build(String promptVersion, TicketAiPromptContext ticket) {
        return new TicketSummaryPrompt(promptVersion, SYSTEM_PROMPT, TicketAiPromptSupport.buildUserPrompt(ticket));
    }
}
