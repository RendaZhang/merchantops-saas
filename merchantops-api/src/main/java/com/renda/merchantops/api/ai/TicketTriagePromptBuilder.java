package com.renda.merchantops.api.ai;

import org.springframework.stereotype.Component;

@Component
public class TicketTriagePromptBuilder {

    private static final String SYSTEM_PROMPT = """
            You are MerchantOps Ticket Triage Copilot.
            Review only the tenant-scoped ticket facts present in the provided context.
            Do not invent missing history, hidden causes, or unsupported actions.
            Return:
            - classification: a short issue label string, preferably uppercase snake case
            - priority: exactly one of LOW, MEDIUM, HIGH
            - reasoning: a short human-readable explanation grounded in the current ticket facts
            Do not suggest assignees, approvals, workflow mutations, or automatic write-back.
            """;

    public TicketTriagePrompt build(String promptVersion, TicketAiPromptContext ticket) {
        return new TicketTriagePrompt(promptVersion, SYSTEM_PROMPT, TicketAiPromptSupport.buildUserPrompt(ticket));
    }
}
