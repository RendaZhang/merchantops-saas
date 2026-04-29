package com.renda.merchantops.api.ai.eval;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

class AiWorkflowEvalComparatorTest {

    @Test
    void generationWorkflowComparatorShouldKeepExecutableGovernanceBaselineStable() throws Exception {
        List<AiWorkflowEvalResult> results = new ArrayList<>();
        for (AiWorkflowEvalInventoryEntry inventoryEntry : AiWorkflowEvalInventory.all()) {
            results.add(inventoryEntry.runner().run(
                    inventoryEntry,
                    EnumSet.allOf(AiWorkflowEvalSampleType.class)
            ));
        }
        AiWorkflowEvalAssertions.assertNoFailures(results);
    }
}
