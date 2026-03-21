package com.renda.merchantops.api.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.config.AiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiTicketSummaryProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generateSummaryShouldSendExpectedRequestAndParseLaterMultipartOutputText() throws Exception {
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
                          "type": "output_image"
                        },
                        {
                          "type": "output_text",
                          "text": "{\\"summary\\":\\"Issue: Printer cable replacement is in progress under ops. "
                        },
                        {
                          "type": "output_text",
                          "text": "Current: the ticket is assigned and the latest signal says cable swap started. Next: confirm the replacement outcome and close the ticket if the printer is healthy.\\"}"
                        }
                      ]
                    }
                  ]
                }
                """, server -> {
            OpenAiTicketSummaryProvider provider = newProvider(server.baseUrl());

            TicketSummaryProviderResult result = provider.generateSummary(sampleRequest());

            assertThat(result.summary()).isEqualTo("Issue: Printer cable replacement is in progress under ops. Current: the ticket is assigned and the latest signal says cable swap started. Next: confirm the replacement outcome and close the ticket if the printer is healthy.");
            assertThat(result.modelId()).isEqualTo("gpt-4.1-mini");
            assertThat(result.totalTokens()).isEqualTo(164);

            JsonNode requestBody = objectMapper.readTree(server.requireCapturedRequest().body());
            assertThat(server.requireCapturedRequest().method()).isEqualTo("POST");
            assertThat(server.requireCapturedRequest().header(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-key");
            assertThat(server.requireCapturedRequest().header("X-Client-Request-Id")).isEqualTo("ticket-ai-summary-provider-test-1");
            assertThat(requestBody.path("model").asText()).isEqualTo("gpt-4.1-mini");
            assertThat(requestBody.path("input").isArray()).isTrue();
            assertThat(requestBody.path("input").size()).isEqualTo(2);
            assertThat(requestBody.path("input").get(0).path("role").asText()).isEqualTo("system");
            assertThat(requestBody.path("input").get(0).path("content").asText()).isEqualTo("system");
            assertThat(requestBody.path("input").get(1).path("role").asText()).isEqualTo("user");
            assertThat(requestBody.path("input").get(1).path("content").asText()).isEqualTo("user");
            assertThat(requestBody.path("text").path("format").path("type").asText()).isEqualTo("json_schema");
            assertThat(requestBody.path("text").path("format").path("strict").asBoolean()).isTrue();
            assertThat(requestBody.path("text").path("format").path("name").asText()).isEqualTo("ticket_summary_response");
            assertThat(objectMapper.convertValue(
                    requestBody.path("text").path("format").path("schema").path("required"),
                    new TypeReference<List<String>>() {
                    }
            )).containsExactly("summary");
        });
    }

    @Test
    void generateSummaryShouldTreatHttp408AsTimeout() throws Exception {
        OpenAiFixtureServer.withServer(408, """
                {
                  "error": {
                    "message": "timeout"
                  }
                }
                """, server -> {
            OpenAiTicketSummaryProvider provider = newProvider(server.baseUrl());

            assertTimeout(provider);
        });
    }

    @Test
    void generateSummaryShouldTreatHttp504AsTimeout() throws Exception {
        OpenAiFixtureServer.withServer(504, """
                {
                  "error": {
                    "message": "gateway timeout"
                  }
                }
                """, server -> {
            OpenAiTicketSummaryProvider provider = newProvider(server.baseUrl());

            assertTimeout(provider);
        });
    }

    @Test
    void generateSummaryShouldRejectUnsupportedContentType() throws Exception {
        OpenAiFixtureServer.withServer(200, """
                {
                  "model": "gpt-4.1-mini",
                  "output": [
                    {
                      "content": [
                        {
                          "type": "output_image"
                        }
                      ]
                    }
                  ]
                }
                """, server -> {
            OpenAiTicketSummaryProvider provider = newProvider(server.baseUrl());

            assertThatThrownBy(() -> provider.generateSummary(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider returned unsupported content");
        });
    }

    @Test
    void generateSummaryShouldRejectRefusalContent() throws Exception {
        OpenAiFixtureServer.withServer(200, """
                {
                  "model": "gpt-4.1-mini",
                  "output": [
                    {
                      "content": [
                        {
                          "type": "refusal"
                        }
                      ]
                    }
                  ]
                }
                """, server -> {
            OpenAiTicketSummaryProvider provider = newProvider(server.baseUrl());

            assertThatThrownBy(() -> provider.generateSummary(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider refused the summary request");
        });
    }

    @Test
    void generateSummaryShouldRejectInvalidJsonPayload() throws Exception {
        OpenAiFixtureServer.withServer(200, """
                {
                  "model": "gpt-4.1-mini",
                  "output": [
                    {
                      "content": [
                        {
                          "type": "output_text",
                          "text": "not-json"
                        }
                      ]
                    }
                  ]
                }
                """, server -> {
            OpenAiTicketSummaryProvider provider = newProvider(server.baseUrl());

            assertThatThrownBy(() -> provider.generateSummary(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider summary payload is invalid");
        });
    }

    @Test
    void generateSummaryShouldRejectMissingSummary() throws Exception {
        OpenAiFixtureServer.withServer(200, """
                {
                  "model": "gpt-4.1-mini",
                  "output": [
                    {
                      "content": [
                        {
                          "type": "output_text",
                          "text": "{}"
                        }
                      ]
                    }
                  ]
                }
                """, server -> {
            OpenAiTicketSummaryProvider provider = newProvider(server.baseUrl());

            assertThatThrownBy(() -> provider.generateSummary(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider summary payload is blank");
        });
    }

    private void assertTimeout(OpenAiTicketSummaryProvider provider) {
        assertThatThrownBy(() -> provider.generateSummary(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("ai provider timed out")
                .satisfies(ex -> assertThat(((AiProviderException) ex).getFailureType()).isEqualTo(AiProviderFailureType.TIMEOUT));
    }

    private OpenAiTicketSummaryProvider newProvider(String baseUrl) {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setModelId("gpt-4.1-mini");
        aiProperties.getOpenai().setApiKey("test-key");
        aiProperties.getOpenai().setBaseUrl(baseUrl);
        return new OpenAiTicketSummaryProvider(RestClient.builder(), new ObjectMapper(), aiProperties);
    }

    private TicketSummaryProviderRequest sampleRequest() {
        return new TicketSummaryProviderRequest(
                "ticket-ai-summary-provider-test-1",
                302L,
                "gpt-4.1-mini",
                1000,
                new TicketSummaryPrompt("ticket-summary-v1", "system", "user")
        );
    }
}
