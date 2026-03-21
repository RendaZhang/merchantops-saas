package com.renda.merchantops.api.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.dto.ticket.query.TicketAiTriagePriority;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiTicketTriageProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generateTriageShouldSendExpectedRequestAndParseLaterMultipartOutputText() throws Exception {
        OpenAiFixtureServer.withServer(200, """
                {
                  "model": "gpt-4.1-mini",
                  "usage": {
                    "input_tokens": 90,
                    "output_tokens": 60,
                    "total_tokens": 150
                  },
                  "output": [
                    {
                      "content": [
                        {
                          "type": "output_image"
                        },
                        {
                          "type": "output_text",
                          "text": "{\\"classification\\":\\"DEVICE_ISSUE\\",\\"priority\\":\\"HIGH\\","
                        },
                        {
                          "type": "output_text",
                          "text": "\\"reasoning\\":\\"The ticket describes a printer outage blocking store operations during peak hours, so it should be treated as a high-priority device issue.\\"}"
                        }
                      ]
                    }
                  ]
                }
                """, server -> {
            OpenAiTicketTriageProvider provider = newProvider(server.baseUrl());

            TicketTriageProviderResult result = provider.generateTriage(sampleRequest());

            assertThat(result.classification()).isEqualTo("DEVICE_ISSUE");
            assertThat(result.priority()).isEqualTo(TicketAiTriagePriority.HIGH);
            assertThat(result.reasoning()).isEqualTo("The ticket describes a printer outage blocking store operations during peak hours, so it should be treated as a high-priority device issue.");
            assertThat(result.totalTokens()).isEqualTo(150);

            JsonNode requestBody = objectMapper.readTree(server.requireCapturedRequest().body());
            assertThat(server.requireCapturedRequest().method()).isEqualTo("POST");
            assertThat(server.requireCapturedRequest().header(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-key");
            assertThat(server.requireCapturedRequest().header("X-Client-Request-Id")).isEqualTo("ticket-ai-triage-provider-test-1");
            assertThat(requestBody.path("model").asText()).isEqualTo("gpt-4.1-mini");
            assertThat(requestBody.path("input").isArray()).isTrue();
            assertThat(requestBody.path("input").size()).isEqualTo(2);
            assertThat(requestBody.path("input").get(0).path("role").asText()).isEqualTo("system");
            assertThat(requestBody.path("input").get(0).path("content").asText()).isEqualTo("system");
            assertThat(requestBody.path("input").get(1).path("role").asText()).isEqualTo("user");
            assertThat(requestBody.path("input").get(1).path("content").asText()).isEqualTo("user");
            assertThat(requestBody.path("text").path("format").path("type").asText()).isEqualTo("json_schema");
            assertThat(requestBody.path("text").path("format").path("strict").asBoolean()).isTrue();
            assertThat(requestBody.path("text").path("format").path("name").asText()).isEqualTo("ticket_triage_response");
            assertThat(objectMapper.convertValue(
                    requestBody.path("text").path("format").path("schema").path("required"),
                    new TypeReference<List<String>>() {
                    }
            )).containsExactlyInAnyOrder("classification", "priority", "reasoning");
        });
    }

    @Test
    void generateTriageShouldTreatHttp408AsTimeout() throws Exception {
        OpenAiFixtureServer.withServer(408, """
                {
                  "error": {
                    "message": "timeout"
                  }
                }
                """, server -> {
            OpenAiTicketTriageProvider provider = newProvider(server.baseUrl());

            assertTimeout(provider);
        });
    }

    @Test
    void generateTriageShouldTreatHttp504AsTimeout() throws Exception {
        OpenAiFixtureServer.withServer(504, """
                {
                  "error": {
                    "message": "gateway timeout"
                  }
                }
                """, server -> {
            OpenAiTicketTriageProvider provider = newProvider(server.baseUrl());

            assertTimeout(provider);
        });
    }

    @Test
    void generateTriageShouldRejectUnsupportedContentType() throws Exception {
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
            OpenAiTicketTriageProvider provider = newProvider(server.baseUrl());

            assertThatThrownBy(() -> provider.generateTriage(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider returned unsupported content");
        });
    }

    @Test
    void generateTriageShouldRejectRefusalContent() throws Exception {
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
            OpenAiTicketTriageProvider provider = newProvider(server.baseUrl());

            assertThatThrownBy(() -> provider.generateTriage(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider refused the triage request");
        });
    }

    @Test
    void generateTriageShouldRejectInvalidJsonPayload() throws Exception {
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
            OpenAiTicketTriageProvider provider = newProvider(server.baseUrl());

            assertThatThrownBy(() -> provider.generateTriage(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider triage payload is invalid");
        });
    }

    @Test
    void generateTriageShouldRejectMissingClassification() throws Exception {
        OpenAiFixtureServer.withServer(200, validPayload("""
                {
                  "priority": "HIGH",
                  "reasoning": "Operations are blocked."
                }
                """), server -> {
            OpenAiTicketTriageProvider provider = newProvider(server.baseUrl());

            assertThatThrownBy(() -> provider.generateTriage(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider triage payload is missing classification");
        });
    }

    @Test
    void generateTriageShouldRejectMissingReasoning() throws Exception {
        OpenAiFixtureServer.withServer(200, validPayload("""
                {
                  "classification": "DEVICE_ISSUE",
                  "priority": "HIGH"
                }
                """), server -> {
            OpenAiTicketTriageProvider provider = newProvider(server.baseUrl());

            assertThatThrownBy(() -> provider.generateTriage(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider triage payload is missing reasoning");
        });
    }

    @Test
    void generateTriageShouldRejectMissingPriority() throws Exception {
        OpenAiFixtureServer.withServer(200, validPayload("""
                {
                  "classification": "DEVICE_ISSUE",
                  "reasoning": "Operations are blocked."
                }
                """), server -> {
            OpenAiTicketTriageProvider provider = newProvider(server.baseUrl());

            assertThatThrownBy(() -> provider.generateTriage(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider triage payload is missing priority");
        });
    }

    @Test
    void generateTriageShouldRejectInvalidPriority() throws Exception {
        OpenAiFixtureServer.withServer(200, validPayload("""
                {
                  "classification": "DEVICE_ISSUE",
                  "priority": "URGENT",
                  "reasoning": "Operations are blocked."
                }
                """), server -> {
            OpenAiTicketTriageProvider provider = newProvider(server.baseUrl());

            assertThatThrownBy(() -> provider.generateTriage(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider triage payload has invalid priority");
        });
    }

    private void assertTimeout(OpenAiTicketTriageProvider provider) {
        assertThatThrownBy(() -> provider.generateTriage(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("ai provider timed out")
                .satisfies(ex -> assertThat(((AiProviderException) ex).getFailureType()).isEqualTo(AiProviderFailureType.TIMEOUT));
    }

    private String validPayload(String payload) {
        return """
                {
                  "model": "gpt-4.1-mini",
                  "output": [
                    {
                      "content": [
                        {
                          "type": "output_text",
                          "text": %s
                        }
                      ]
                    }
                  ]
                }
                """.formatted(objectMapperText(payload));
    }

    private String objectMapperText(String payload) {
        return "\"" + payload
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n")
                + "\"";
    }

    private OpenAiTicketTriageProvider newProvider(String baseUrl) {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setModelId("gpt-4.1-mini");
        aiProperties.getOpenai().setApiKey("test-key");
        aiProperties.getOpenai().setBaseUrl(baseUrl);
        return new OpenAiTicketTriageProvider(RestClient.builder(), new ObjectMapper(), aiProperties);
    }

    private TicketTriageProviderRequest sampleRequest() {
        return new TicketTriageProviderRequest(
                "ticket-ai-triage-provider-test-1",
                302L,
                "gpt-4.1-mini",
                1000,
                new TicketTriagePrompt("ticket-triage-v1", "system", "user")
        );
    }
}
