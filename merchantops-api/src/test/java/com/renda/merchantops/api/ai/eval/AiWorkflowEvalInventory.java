package com.renda.merchantops.api.ai.eval;

import com.renda.merchantops.api.ai.core.AiGenerationWorkflow;

import java.util.List;

public final class AiWorkflowEvalInventory {

    private static final List<AiWorkflowEvalInventoryEntry> ENTRIES = List.of(
            entry(AiGenerationWorkflow.TICKET_SUMMARY, "ticket-summary-v1", new TicketSummaryEvalRunner()),
            entry(AiGenerationWorkflow.TICKET_TRIAGE, "ticket-triage-v1", new TicketTriageEvalRunner()),
            entry(AiGenerationWorkflow.TICKET_REPLY_DRAFT, "ticket-reply-draft-v1", new TicketReplyDraftEvalRunner()),
            entry(AiGenerationWorkflow.IMPORT_ERROR_SUMMARY, "import-error-summary-v1",
                    new ImportJobErrorSummaryEvalRunner()),
            entry(AiGenerationWorkflow.IMPORT_MAPPING_SUGGESTION, "import-mapping-suggestion-v1",
                    new ImportJobMappingSuggestionEvalRunner()),
            entry(AiGenerationWorkflow.IMPORT_FIX_RECOMMENDATION, "import-fix-recommendation-v1",
                    new ImportJobFixRecommendationEvalRunner())
    );

    private AiWorkflowEvalInventory() {
    }

    public static List<AiWorkflowEvalInventoryEntry> all() {
        return ENTRIES;
    }

    public static AiWorkflowEvalInventoryEntry forWorkflow(AiGenerationWorkflow workflow) {
        return ENTRIES.stream()
                .filter(entry -> entry.workflow() == workflow)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No eval inventory entry for workflow " + workflow));
    }

    private static AiWorkflowEvalInventoryEntry entry(AiGenerationWorkflow workflow,
                                                      String expectedPromptVersion,
                                                      AiWorkflowEvalRunner runner) {
        String workflowDir = "/ai/" + workflow.workflowName();
        return new AiWorkflowEvalInventoryEntry(
                workflow,
                // Eval expectations are a fixed governance baseline, not a mirror of the runtime default under test.
                expectedPromptVersion,
                workflowDir + "/golden-samples.json",
                workflowDir + "/failure-samples.json",
                workflowDir + "/policy-samples.json",
                runner
        );
    }
}
