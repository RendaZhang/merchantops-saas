package com.renda.merchantops.api.ai.eval;

import java.util.List;

public record AiWorkflowEvalResult(
        AiWorkflowEvalInventoryEntry inventoryEntry,
        int goldenCount,
        int failureCount,
        int policyCount,
        List<AiWorkflowEvalFailure> failures
) {
}
