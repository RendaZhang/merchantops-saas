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

class OpenAiTicketReplyDraftProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generateReplyDraftShouldSendExpectedRequestAndParseLaterMultipartOutputText() throws Exception {
        OpenAiFixtureServer.withServer(200, """
                {
                  "model": "gpt-4.1-mini",
                  "usage": {
                    "input_tokens": 110,
                    "output_tokens": 70,
                    "total_tokens": 180
                  },
                  "output": [
                    {
                      "content": [
                        {
                          "type": "output_image"
                        },
                        {
                          "type": "output_text",
                          "text": "{\\"opening\\":\\"Quick update from ops.\\",\\"body\\":\\"The ticket remains in progress and the latest internal signal confirms the cable swap has started for the printer issue.\\","
                        },
                        {
                          "type": "output_text",
                          "text": "\\"nextStep\\":\\"Confirm whether the replacement restored printer health and capture any blocker before moving toward closure.\\",\\"closing\\":\\"I will add another internal note once the verification result is confirmed.\\"}"
                        }
                      ]
                    }
                  ]
                }
                """, server -> {
            OpenAiTicketReplyDraftProvider provider = newProvider(server.baseUrl());

            TicketReplyDraftProviderResult result = provider.generateReplyDraft(sampleRequest());

            assertThat(result.opening()).isEqualTo("Quick update from ops.");
            assertThat(result.body()).isEqualTo("The ticket remains in progress and the latest internal signal confirms the cable swap has started for the printer issue.");
            assertThat(result.nextStep()).isEqualTo("Confirm whether the replacement restored printer health and capture any blocker before moving toward closure.");
            assertThat(result.closing()).isEqualTo("I will add another internal note once the verification result is confirmed.");
            assertThat(result.totalTokens()).isEqualTo(180);

            JsonNode requestBody = objectMapper.readTree(server.requireCapturedRequest().body());
            assertThat(server.requireCapturedRequest().method()).isEqualTo("POST");
            assertThat(server.requireCapturedRequest().header(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer test-key");
            assertThat(server.requireCapturedRequest().header("X-Client-Request-Id")).isEqualTo("ticket-ai-reply-draft-provider-test-1");
            assertThat(requestBody.path("model").asText()).isEqualTo("gpt-4.1-mini");
            assertThat(requestBody.path("input").isArray()).isTrue();
            assertThat(requestBody.path("input").size()).isEqualTo(2);
            assertThat(requestBody.path("input").get(0).path("role").asText()).isEqualTo("system");
            assertThat(requestBody.path("input").get(0).path("content").asText()).isEqualTo("system");
            assertThat(requestBody.path("input").get(1).path("role").asText()).isEqualTo("user");
            assertThat(requestBody.path("input").get(1).path("content").asText()).isEqualTo("user");
            assertThat(requestBody.path("text").path("format").path("type").asText()).isEqualTo("json_schema");
            assertThat(requestBody.path("text").path("format").path("strict").asBoolean()).isTrue();
            assertThat(requestBody.path("text").path("format").path("name").asText()).isEqualTo("ticket_reply_draft_response");
            assertThat(objectMapper.convertValue(
                    requestBody.path("text").path("format").path("schema").path("required"),
                    new TypeReference<List<String>>() {
                    }
            )).containsExactlyInAnyOrder("opening", "body", "nextStep", "closing");
        });
    }

    @Test
    void generateReplyDraftShouldTreatHttp408AsTimeout() throws Exception {
        OpenAiFixtureServer.withServer(408, """
                {
                  "error": {
                    "message": "timeout"
                  }
                }
                """, server -> {
            OpenAiTicketReplyDraftProvider provider = newProvider(server.baseUrl());

            assertTimeout(provider);
        });
    }

    @Test
    void generateReplyDraftShouldTreatHttp504AsTimeout() throws Exception {
        OpenAiFixtureServer.withServer(504, """
                {
                  "error": {
                    "message": "gateway timeout"
                  }
                }
                """, server -> {
            OpenAiTicketReplyDraftProvider provider = newProvider(server.baseUrl());

            assertTimeout(provider);
        });
    }

    @Test
    void generateReplyDraftShouldRejectUnsupportedContentType() throws Exception {
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
            OpenAiTicketReplyDraftProvider provider = newProvider(server.baseUrl());

            assertThatThrownBy(() -> provider.generateReplyDraft(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider returned unsupported content");
        });
    }

    @Test
    void generateReplyDraftShouldRejectRefusalContent() throws Exception {
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
            OpenAiTicketReplyDraftProvider provider = newProvider(server.baseUrl());

            assertThatThrownBy(() -> provider.generateReplyDraft(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider refused the reply draft request");
        });
    }

    @Test
    void generateReplyDraftShouldRejectInvalidJsonPayload() throws Exception {
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
            OpenAiTicketReplyDraftProvider provider = newProvider(server.baseUrl());

            assertThatThrownBy(() -> provider.generateReplyDraft(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider reply draft payload is invalid");
        });
    }

    @Test
    void generateReplyDraftShouldRejectMissingOpening() throws Exception {
        assertMissingField("opening");
    }

    @Test
    void generateReplyDraftShouldRejectMissingBody() throws Exception {
        assertMissingField("body");
    }

    @Test
    void generateReplyDraftShouldRejectMissingNextStep() throws Exception {
        assertMissingField("nextStep");
    }

    @Test
    void generateReplyDraftShouldRejectMissingClosing() throws Exception {
        assertMissingField("closing");
    }

    private void assertMissingField(String fieldName) throws Exception {
        OpenAiFixtureServer.withServer(200, validPayload(missingFieldPayload(fieldName)), server -> {
            OpenAiTicketReplyDraftProvider provider = newProvider(server.baseUrl());

            assertThatThrownBy(() -> provider.generateReplyDraft(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider reply draft payload is missing " + fieldName);
        });
    }

    private String missingFieldPayload(String fieldName) {
        String opening = "\"Quick update from ops.\"";
        String body = "\"The ticket remains in progress and the latest internal signal confirms the cable swap has started for the printer issue.\"";
        String nextStep = "\"Confirm whether the replacement restored printer health and capture any blocker before moving toward closure.\"";
        String closing = "\"I will add another internal note once the verification result is confirmed.\"";

        return """
                {
                  %s
                }
                """.formatted(
                switch (fieldName) {
                    case "opening" -> """
                            "body": %s,
                            "nextStep": %s,
                            "closing": %s
                            """.formatted(body, nextStep, closing);
                    case "body" -> """
                            "opening": %s,
                            "nextStep": %s,
                            "closing": %s
                            """.formatted(opening, nextStep, closing);
                    case "nextStep" -> """
                            "opening": %s,
                            "body": %s,
                            "closing": %s
                            """.formatted(opening, body, closing);
                    case "closing" -> """
                            "opening": %s,
                            "body": %s,
                            "nextStep": %s
                            """.formatted(opening, body, nextStep);
                    default -> throw new IllegalArgumentException("unsupported field: " + fieldName);
                }
        );
    }

    private void assertTimeout(OpenAiTicketReplyDraftProvider provider) {
        assertThatThrownBy(() -> provider.generateReplyDraft(sampleRequest()))
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

    private OpenAiTicketReplyDraftProvider newProvider(String baseUrl) {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setModelId("gpt-4.1-mini");
        aiProperties.getOpenai().setApiKey("test-key");
        aiProperties.getOpenai().setBaseUrl(baseUrl);
        return new OpenAiTicketReplyDraftProvider(RestClient.builder(), new ObjectMapper(), aiProperties);
    }

    private TicketReplyDraftProviderRequest sampleRequest() {
        return new TicketReplyDraftProviderRequest(
                "ticket-ai-reply-draft-provider-test-1",
                302L,
                "gpt-4.1-mini",
                1000,
                new TicketReplyDraftPrompt("ticket-reply-draft-v1", "system", "user")
        );
    }
}
