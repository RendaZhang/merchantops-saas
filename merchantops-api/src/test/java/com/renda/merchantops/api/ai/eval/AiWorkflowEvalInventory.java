package com.renda.merchantops.api.ai.eval;

import com.renda.merchantops.api.ai.core.AiGenerationWorkflow;

import java.util.List;

public final class AiWorkflowEvalInventory {

    private static final List<AiWorkflowEvalInventoryEntry> ENTRIES = List.of(
            entry(AiGenerationWorkflow.TICKET_SUMMARY, new TicketSummaryEvalRunner()),
            entry(AiGenerationWorkflow.TICKET_TRIAGE, new TicketTriageEvalRunner()),
            entry(AiGenerationWorkflow.TICKET_REPLY_DRAFT, new TicketReplyDraftEvalRunner()),
            entry(AiGenerationWorkflow.IMPORT_ERROR_SUMMARY, new ImportJobErrorSummaryEvalRunner()),
            entry(AiGenerationWorkflow.IMPORT_MAPPING_SUGGESTION, new ImportJobMappingSuggestionEvalRunner()),
            entry(AiGenerationWorkflow.IMPORT_FIX_RECOMMENDATION, new ImportJobFixRecommendationEvalRunner())
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

    private static AiWorkflowEvalInventoryEntry entry(AiGenerationWorkflow workflow, AiWorkflowEvalRunner runner) {
        String workflowDir = "/ai/" + workflow.workflowName();
        return new AiWorkflowEvalInventoryEntry(
                workflow,
                workflow.defaultPromptVersion(),
                workflowDir + "/golden-samples.json",
                workflowDir + "/failure-samples.json",
                workflowDir + "/policy-samples.json",
                runner
        );
    }
}
