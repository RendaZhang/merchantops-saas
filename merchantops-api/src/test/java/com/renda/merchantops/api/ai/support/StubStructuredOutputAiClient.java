package com.renda.merchantops.api.ai.support;

import com.renda.merchantops.api.ai.client.StructuredOutputAiClient;
import com.renda.merchantops.api.ai.client.StructuredOutputAiRequest;
import com.renda.merchantops.api.ai.client.StructuredOutputAiResponse;

public final class StubStructuredOutputAiClient implements StructuredOutputAiClient {

    private StructuredOutputAiRequest lastRequest;
    private StructuredOutputAiResponse response;
    private RuntimeException failure;

    @Override
    public StructuredOutputAiResponse generate(StructuredOutputAiRequest request) {
        lastRequest = request;
        if (failure != null) {
            throw failure;
        }
        return response;
    }

    public void willReturn(StructuredOutputAiResponse value) {
        this.response = value;
        this.failure = null;
    }

    public void willThrow(RuntimeException value) {
        this.failure = value;
        this.response = null;
    }

    public StructuredOutputAiRequest lastRequest() {
        return lastRequest;
    }
}
