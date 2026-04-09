package com.renda.merchantops.api.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.core.AiProviderFailureType;
import com.renda.merchantops.api.ai.support.OpenAiFixtureServer;
import com.renda.merchantops.api.config.AiClientConfig;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.config.AiProviderType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringAiOpenAiStructuredOutputAiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiClientConfig aiClientConfig = new AiClientConfig();

    @Test
    void generateShouldSendChatCompletionsJsonSchemaRequestAndParseUsageMetadata() throws Exception {
        OpenAiFixtureServer.withServer("/v1/chat/completions", 200, """
                {
                  "id": "chatcmpl-1",
                  "model": "gpt-4.1-mini-2025-04-14",
                  "usage": {
                    "prompt_tokens": 120,
                    "completion_tokens": 44,
                    "total_tokens": 164
                  },
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "{\\"summary\\":\\"hello world\\"}"
                      },
                      "finish_reason": "stop"
                    }
                  ]
                }
                """, server -> {
            SpringAiOpenAiStructuredOutputAiClient client = newClient(server.baseUrl());

            StructuredOutputAiResponse response = client.generate(sampleRequest());

            assertThat(response.rawText()).isEqualTo("{\"summary\":\"hello world\"}");
            assertThat(response.modelId()).isEqualTo("gpt-4.1-mini-2025-04-14");
            assertThat(response.inputTokens()).isEqualTo(120);
            assertThat(response.outputTokens()).isEqualTo(44);
            assertThat(response.totalTokens()).isEqualTo(164);
            assertThat(response.costMicros()).isNull();

            JsonNode requestBody = objectMapper.readTree(server.requireCapturedRequest().body());
            assertThat(server.requireCapturedRequest().method()).isEqualTo("POST");
            assertThat(server.requireCapturedRequest().header(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-key");
            assertThat(server.requireCapturedRequest().header("X-Client-Request-Id")).isEqualTo("req-1");
            assertThat(requestBody.path("model").asText()).isEqualTo("gpt-4.1-mini");
            assertThat(requestBody.path("response_format").path("type").asText()).isEqualTo("json_schema");
            assertThat(requestBody.path("response_format").path("json_schema").path("name").asText()).isEqualTo("summary_shape");
            assertThat(requestBody.path("response_format").path("json_schema").path("strict").asBoolean()).isTrue();
            assertThat(requestBody.path("max_tokens").asInt()).isEqualTo(220);
            assertThat(requestBody.path("messages").get(0).path("role").asText()).isEqualTo("system");
            assertThat(requestBody.path("messages").get(0).path("content").asText()).isEqualTo("system");
            assertThat(requestBody.path("messages").get(1).path("role").asText()).isEqualTo("user");
            assertThat(requestBody.path("messages").get(1).path("content").asText()).isEqualTo("user");
        });
    }

    @Test
    void generateShouldRejectBlankMessageContentAsInvalidResponse() throws Exception {
        OpenAiFixtureServer.withServer("/v1/chat/completions", 200, """
                {
                  "id": "chatcmpl-1",
                  "model": "gpt-4.1-mini",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": ""
                      },
                      "finish_reason": "stop"
                    }
                  ]
                }
                """, server -> {
            SpringAiOpenAiStructuredOutputAiClient client = newClient(server.baseUrl());

            assertThatThrownBy(() -> client.generate(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider returned blank content")
                    .satisfies(ex -> assertThat(((AiProviderException) ex).getFailureType())
                            .isEqualTo(AiProviderFailureType.INVALID_RESPONSE));
        });
    }

    @Test
    void generateShouldTreatHttp408AsTimeout() throws Exception {
        OpenAiFixtureServer.withServer("/v1/chat/completions", 408, """
                {
                  "error": {
                    "message": "timeout"
                  }
                }
                """, server -> {
            SpringAiOpenAiStructuredOutputAiClient client = newClient(server.baseUrl());

            assertThatThrownBy(() -> client.generate(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("ai provider timed out")
                    .satisfies(ex -> assertThat(((AiProviderException) ex).getFailureType())
                            .isEqualTo(AiProviderFailureType.TIMEOUT));
        });
    }

    @Test
    void generateShouldTreatInvalidHttpResponseAsUnavailable() throws Exception {
        OpenAiFixtureServer.withServer("/v1/chat/completions", 503, """
                {
                  "error": {
                    "message": "service unavailable"
                  }
                }
                """, server -> {
            SpringAiOpenAiStructuredOutputAiClient client = newClient(server.baseUrl());

            assertThatThrownBy(() -> client.generate(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("ai provider returned an error")
                    .satisfies(ex -> assertThat(((AiProviderException) ex).getFailureType())
                            .isEqualTo(AiProviderFailureType.UNAVAILABLE));
        });
    }

    private SpringAiOpenAiStructuredOutputAiClient newClient(String baseUrl) {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setProvider(AiProviderType.OPENAI);
        aiProperties.setBaseUrl(baseUrl);
        aiProperties.setApiKey("test-key");

        RetryTemplate retryTemplate = aiClientConfig.aiProviderRetryTemplate();
        return new SpringAiOpenAiStructuredOutputAiClient(
                RestClient.builder(),
                aiProperties,
                retryTemplate,
                aiClientConfig.aiProviderToolCallingManager()
        );
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
