package com.renda.merchantops.api.ai.eval;

import com.renda.merchantops.api.ai.core.AiGenerationWorkflow;

public record AiWorkflowEvalInventoryEntry(
        AiGenerationWorkflow workflow,
        String expectedPromptVersion,
        String goldenSamplesPath,
        String failureSamplesPath,
        String policySamplesPath,
        AiWorkflowEvalRunner runner
) {
}
