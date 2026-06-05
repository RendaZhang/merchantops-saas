package com.renda.merchantops.api.ai.eval;

public record AiWorkflowEvalFailure(
        String workflowName,
        AiWorkflowEvalSampleType sampleType,
        String sampleId,
        String message
) {
}
