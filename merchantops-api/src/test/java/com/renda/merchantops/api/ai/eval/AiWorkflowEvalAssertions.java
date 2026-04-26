package com.renda.merchantops.api.ai.eval;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public final class AiWorkflowEvalAssertions {

    private AiWorkflowEvalAssertions() {
    }

    public static void assertNoFailures(List<AiWorkflowEvalResult> results) {
        String report = AiWorkflowEvalReportRenderer.render(results);
        System.out.println(report);
        assertThat(results.stream().flatMap(result -> result.failures().stream()).toList())
                .withFailMessage(report)
                .isEmpty();
    }

    public static void assertNoFailures(AiWorkflowEvalResult result) {
        assertNoFailures(List.of(result));
    }
}
