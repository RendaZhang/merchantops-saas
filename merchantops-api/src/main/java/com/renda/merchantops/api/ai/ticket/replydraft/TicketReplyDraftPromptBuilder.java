package com.renda.merchantops.api.ai.ticket.replydraft;

import com.renda.merchantops.api.ai.ticket.TicketAiPromptSupport;
import com.renda.merchantops.domain.ticket.TicketPromptContext;
import org.springframework.stereotype.Component;

@Component
public class TicketReplyDraftPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            You are MerchantOps Ticket Reply Draft Copilot.
            Draft an internal ticket comment suggestion using only the tenant-scoped ticket facts in the provided context.
            Do not invent missing details, hidden causes, customer identity, external messages, or unsupported actions.
            This is not an external email or customer reply. It is an internal operator comment draft that a human may copy into the existing ticket comment flow.
            Return plain text for these required fields only:
            - opening
            - body
            - nextStep
            - closing
            Do not include labels such as Opening:, Body:, Next step:, or Closing: in any field.
            Keep the tone practical, concise, and workflow-oriented.
            Do not suggest automatic write-back, approval execution, or status mutation.
            """;

    public TicketReplyDraftPrompt build(String promptVersion, TicketPromptContext ticket) {
        return new TicketReplyDraftPrompt(promptVersion, SYSTEM_PROMPT, TicketAiPromptSupport.buildUserPrompt(ticket));
    }
}
