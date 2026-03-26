package com.renda.merchantops.api.ai.core;

public interface AiUsageAwareResult {

    String modelId();

    Integer inputTokens();

    Integer outputTokens();

    Integer totalTokens();

    Long costMicros();
}
