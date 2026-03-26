package com.renda.merchantops.api.ai.client;

import com.fasterxml.jackson.databind.JsonNode;

public final class OpenAiTestResponseSupport {

    private OpenAiTestResponseSupport() {
    }

    public static String extractOutputText(JsonNode response) {
        return AiProviderHttpSupport.extractOpenAiOutputText(response.path("output")).text();
    }
}
