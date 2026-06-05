package com.renda.merchantops.api.ai.ticket.replydraft;

import com.renda.merchantops.api.ai.core.AiGenerationWorkflow;
import com.renda.merchantops.api.ai.eval.AiWorkflowEvalAssertions;
import com.renda.merchantops.api.ai.eval.AiWorkflowEvalInventory;
import com.renda.merchantops.api.ai.eval.AiWorkflowEvalInventoryEntry;
import com.renda.merchantops.api.ai.eval.AiWorkflowEvalSampleType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

class TicketReplyDraftGoldenSampleTest {

    @Test
    void goldenSamplesShouldKeepStableReplyDraftFormatThroughSharedEvalRunner() throws Exception {
        AiWorkflowEvalInventoryEntry inventoryEntry = AiWorkflowEvalInventory.forWorkflow(AiGenerationWorkflow.TICKET_REPLY_DRAFT);
        AiWorkflowEvalAssertions.assertNoFailures(
                inventoryEntry.runner().run(inventoryEntry, EnumSet.of(AiWorkflowEvalSampleType.GOLDEN))
        );
    }
}
