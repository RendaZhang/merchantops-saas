package com.renda.merchantops.api.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.core.AiProviderFailureType;
import com.renda.merchantops.api.ai.support.OpenAiFixtureServer;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.config.AiProviderType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiResponsesStructuredOutputAiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generateShouldSendResponsesSchemaRequestAndParseMultipartOutputText() throws Exception {
        OpenAiFixtureServer.withServer(200, """
                {
                  "model": "gpt-4.1-mini",
                  "usage": {
                    "input_tokens": 120,
                    "output_tokens": 44,
                    "total_tokens": 164
                  },
                  "output": [
                    {
                      "content": [
                        {
                          "type": "output_text",
                          "text": "{\\"summary\\":\\"hello "
                        },
                        {
                          "type": "output_text",
                          "text": "world\\"}"
                        }
                      ]
                    }
                  ]
                }
                """, server -> {
            OpenAiResponsesStructuredOutputAiClient client = newClient(server.baseUrl());

            StructuredOutputAiResponse response = client.generate(sampleRequest());

            assertThat(response.rawText()).isEqualTo("{\"summary\":\"hello world\"}");
            assertThat(response.modelId()).isEqualTo("gpt-4.1-mini");
            assertThat(response.totalTokens()).isEqualTo(164);

            JsonNode requestBody = objectMapper.readTree(server.requireCapturedRequest().body());
            assertThat(server.requireCapturedRequest().method()).isEqualTo("POST");
            assertThat(server.requireCapturedRequest().header(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-key");
            assertThat(server.requireCapturedRequest().header("X-Client-Request-Id")).isEqualTo("req-1");
            assertThat(requestBody.path("model").asText()).isEqualTo("gpt-4.1-mini");
            assertThat(requestBody.path("text").path("format").path("type").asText()).isEqualTo("json_schema");
            assertThat(requestBody.path("text").path("format").path("strict").asBoolean()).isTrue();
            assertThat(requestBody.path("text").path("format").path("name").asText()).isEqualTo("summary_shape");
            assertThat(requestBody.path("input").get(0).path("content").asText()).isEqualTo("system");
            assertThat(requestBody.path("input").get(1).path("content").asText()).isEqualTo("user");
        });
    }

    @Test
    void generateShouldTreatHttp408AsTimeout() throws Exception {
        OpenAiFixtureServer.withServer(408, """
                {
                  "error": {
                    "message": "timeout"
                  }
                }
                """, server -> {
            OpenAiResponsesStructuredOutputAiClient client = newClient(server.baseUrl());

            assertThatThrownBy(() -> client.generate(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("ai provider timed out")
                    .satisfies(ex -> assertThat(((AiProviderException) ex).getFailureType()).isEqualTo(AiProviderFailureType.TIMEOUT));
        });
    }

    private OpenAiResponsesStructuredOutputAiClient newClient(String baseUrl) {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setProvider(AiProviderType.OPENAI);
        aiProperties.setBaseUrl(baseUrl);
        aiProperties.setApiKey("test-key");
        return new OpenAiResponsesStructuredOutputAiClient(RestClient.builder(), aiProperties);
    }

    private StructuredOutputAiRequest sampleRequest() {
        return new StructuredOutputAiRequest(
                "req-1",
                "gpt-4.1-mini",
                1000,
                "system",
                "user",
                220,
                "summary_shape",
                Map.of(
                        "type", "object",
                        "properties", Map.of("summary", Map.of("type", "string")),
                        "required", List.of("summary"),
                        "additionalProperties", false
                ),
                "{\"summary\":\"example\"}"
        );
    }
}
