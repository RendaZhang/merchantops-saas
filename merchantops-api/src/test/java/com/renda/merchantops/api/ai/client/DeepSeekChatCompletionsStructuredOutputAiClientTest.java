package com.renda.merchantops.api.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.ai.core.AiProviderException;
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

class DeepSeekChatCompletionsStructuredOutputAiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generateShouldSendChatCompletionsJsonModeRequestAndParseMessageContent() throws Exception {
        OpenAiFixtureServer.withServer("/chat/completions", 200, """
                {
                  "id": "chatcmpl-1",
                  "model": "deepseek-chat",
                  "usage": {
                    "prompt_tokens": 90,
                    "completion_tokens": 31,
                    "total_tokens": 121
                  },
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "{\\"summary\\":\\"deepseek summary\\"}"
                      }
                    }
                  ]
                }
                """, server -> {
            DeepSeekChatCompletionsStructuredOutputAiClient client = newClient(server.baseUrl());

            StructuredOutputAiResponse response = client.generate(sampleRequest());

            assertThat(response.rawText()).isEqualTo("{\"summary\":\"deepseek summary\"}");
            assertThat(response.modelId()).isEqualTo("deepseek-chat");
            assertThat(response.inputTokens()).isEqualTo(90);
            assertThat(response.outputTokens()).isEqualTo(31);
            assertThat(response.totalTokens()).isEqualTo(121);

            JsonNode requestBody = objectMapper.readTree(server.requireCapturedRequest().body());
            assertThat(server.requireCapturedRequest().header(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-deepseek-key");
            assertThat(server.requireCapturedRequest().header("X-Client-Request-Id")).isEqualTo("req-1");
            assertThat(requestBody.path("model").asText()).isEqualTo("deepseek-chat");
            assertThat(requestBody.path("response_format").path("type").asText()).isEqualTo("json_object");
            assertThat(requestBody.path("messages").get(0).path("role").asText()).isEqualTo("system");
            assertThat(requestBody.path("messages").get(0).path("content").asText()).contains("Return only a valid JSON object.");
            assertThat(requestBody.path("messages").get(0).path("content").asText()).contains("{\"summary\":\"example\"}");
            assertThat(requestBody.path("messages").get(1).path("content").asText()).isEqualTo("user");
        });
    }

    @Test
    void generateShouldRejectBlankMessageContent() throws Exception {
        OpenAiFixtureServer.withServer("/chat/completions", 200, """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": ""
                      }
                    }
                  ]
                }
                """, server -> {
            DeepSeekChatCompletionsStructuredOutputAiClient client = newClient(server.baseUrl());

            assertThatThrownBy(() -> client.generate(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider returned blank content");
        });
    }

    private DeepSeekChatCompletionsStructuredOutputAiClient newClient(String baseUrl) {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setProvider(AiProviderType.DEEPSEEK);
        aiProperties.setBaseUrl(baseUrl);
        aiProperties.setApiKey("test-deepseek-key");
        return new DeepSeekChatCompletionsStructuredOutputAiClient(RestClient.builder(), aiProperties);
    }

    private StructuredOutputAiRequest sampleRequest() {
        return new StructuredOutputAiRequest(
                "req-1",
                "deepseek-chat",
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
