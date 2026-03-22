package com.renda.merchantops.api.ai;

final class StubStructuredOutputAiClient implements StructuredOutputAiClient {

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

    void willReturn(StructuredOutputAiResponse value) {
        this.response = value;
        this.failure = null;
    }

    void willThrow(RuntimeException value) {
        this.failure = value;
        this.response = null;
    }

    StructuredOutputAiRequest lastRequest() {
        return lastRequest;
    }
}
