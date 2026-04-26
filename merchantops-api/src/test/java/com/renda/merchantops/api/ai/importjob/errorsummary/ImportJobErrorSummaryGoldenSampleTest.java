package com.renda.merchantops.api.ai.importjob.errorsummary;

import com.renda.merchantops.api.ai.core.AiGenerationWorkflow;
import com.renda.merchantops.api.ai.eval.AiWorkflowEvalAssertions;
import com.renda.merchantops.api.ai.eval.AiWorkflowEvalInventory;
import com.renda.merchantops.api.ai.eval.AiWorkflowEvalInventoryEntry;
import com.renda.merchantops.api.ai.eval.AiWorkflowEvalSampleType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

class ImportJobErrorSummaryGoldenSampleTest {

    @Test
    void goldenSamplesShouldKeepStableImportErrorSummaryFormatThroughSharedEvalRunner() throws Exception {
        AiWorkflowEvalInventoryEntry inventoryEntry = AiWorkflowEvalInventory.forWorkflow(AiGenerationWorkflow.IMPORT_ERROR_SUMMARY);
        AiWorkflowEvalAssertions.assertNoFailures(
                inventoryEntry.runner().run(inventoryEntry, EnumSet.of(AiWorkflowEvalSampleType.GOLDEN))
        );
    }
}
