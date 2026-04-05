package com.renda.merchantops.api.ai.eval;

import java.util.List;

public final class AiWorkflowEvalReportRenderer {

    private AiWorkflowEvalReportRenderer() {
    }

    public static String render(List<AiWorkflowEvalResult> results) {
        StringBuilder report = new StringBuilder("AI workflow eval comparator summary\n");
        for (AiWorkflowEvalResult result : results) {
            report.append("- ")
                    .append(result.inventoryEntry().workflow().workflowName())
                    .append(" [interactionType=")
                    .append(result.inventoryEntry().workflow().interactionType())
                    .append(", promptVersion=")
                    .append(result.inventoryEntry().expectedPromptVersion())
                    .append("] golden=")
                    .append(result.goldenCount())
                    .append(", failure=")
                    .append(result.failureCount())
                    .append(", policy=")
                    .append(result.policyCount())
                    .append(", assertionFailures=")
                    .append(result.failures().size())
                    .append('\n');
            for (AiWorkflowEvalFailure failure : result.failures()) {
                report.append("  * ")
                        .append(failure.sampleType())
                        .append(" / ")
                        .append(failure.sampleId())
                        .append(": ")
                        .append(failure.message())
                        .append('\n');
            }
        }
        return report.toString().trim();
    }
}
