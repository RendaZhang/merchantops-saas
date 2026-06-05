package com.renda.merchantops.api.ai.eval;

import java.util.EnumSet;

public interface AiWorkflowEvalRunner {

    AiWorkflowEvalResult run(AiWorkflowEvalInventoryEntry inventoryEntry,
                             EnumSet<AiWorkflowEvalSampleType> sampleTypes) throws Exception;
}
