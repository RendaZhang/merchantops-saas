package com.renda.merchantops.api.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.config.AiProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiTicketTriageProviderTest {

    @Test
    void generateTriageShouldRejectUnsupportedContentType() throws Exception {
        withServer("""
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
                """, baseUrl -> {
            OpenAiTicketTriageProvider provider = newProvider(baseUrl);

            assertThatThrownBy(() -> provider.generateTriage(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider returned unsupported content");
        });
    }

    @Test
    void generateTriageShouldRejectRefusalContent() throws Exception {
        withServer("""
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
                """, baseUrl -> {
            OpenAiTicketTriageProvider provider = newProvider(baseUrl);

            assertThatThrownBy(() -> provider.generateTriage(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider refused the triage request");
        });
    }

    @Test
    void generateTriageShouldRejectInvalidJsonPayload() throws Exception {
        withServer("""
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
                """, baseUrl -> {
            OpenAiTicketTriageProvider provider = newProvider(baseUrl);

            assertThatThrownBy(() -> provider.generateTriage(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider triage payload is invalid");
        });
    }

    @Test
    void generateTriageShouldRejectMissingClassification() throws Exception {
        withServer(validPayload("""
                {
                  "priority": "HIGH",
                  "reasoning": "Operations are blocked."
                }
                """), baseUrl -> {
            OpenAiTicketTriageProvider provider = newProvider(baseUrl);

            assertThatThrownBy(() -> provider.generateTriage(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider triage payload is missing classification");
        });
    }

    @Test
    void generateTriageShouldRejectMissingReasoning() throws Exception {
        withServer(validPayload("""
                {
                  "classification": "DEVICE_ISSUE",
                  "priority": "HIGH"
                }
                """), baseUrl -> {
            OpenAiTicketTriageProvider provider = newProvider(baseUrl);

            assertThatThrownBy(() -> provider.generateTriage(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider triage payload is missing reasoning");
        });
    }

    @Test
    void generateTriageShouldRejectMissingPriority() throws Exception {
        withServer(validPayload("""
                {
                  "classification": "DEVICE_ISSUE",
                  "reasoning": "Operations are blocked."
                }
                """), baseUrl -> {
            OpenAiTicketTriageProvider provider = newProvider(baseUrl);

            assertThatThrownBy(() -> provider.generateTriage(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider triage payload is missing priority");
        });
    }

    @Test
    void generateTriageShouldRejectInvalidPriority() throws Exception {
        withServer(validPayload("""
                {
                  "classification": "DEVICE_ISSUE",
                  "priority": "URGENT",
                  "reasoning": "Operations are blocked."
                }
                """), baseUrl -> {
            OpenAiTicketTriageProvider provider = newProvider(baseUrl);

            assertThatThrownBy(() -> provider.generateTriage(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider triage payload has invalid priority");
        });
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

    private void withServer(String responseBody, ThrowingConsumer<String> consumer) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/responses", exchange -> {
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        });
        server.start();
        try {
            consumer.accept("http://127.0.0.1:" + server.getAddress().getPort());
        } finally {
            server.stop(0);
        }
    }

    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T value) throws IOException;
    }
}
