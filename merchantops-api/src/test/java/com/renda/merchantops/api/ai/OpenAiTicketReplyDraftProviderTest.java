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

class OpenAiTicketReplyDraftProviderTest {

    @Test
    void generateReplyDraftShouldRejectUnsupportedContentType() throws Exception {
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
            OpenAiTicketReplyDraftProvider provider = newProvider(baseUrl);

            assertThatThrownBy(() -> provider.generateReplyDraft(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider returned unsupported content");
        });
    }

    @Test
    void generateReplyDraftShouldRejectRefusalContent() throws Exception {
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
            OpenAiTicketReplyDraftProvider provider = newProvider(baseUrl);

            assertThatThrownBy(() -> provider.generateReplyDraft(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider refused the reply draft request");
        });
    }

    @Test
    void generateReplyDraftShouldRejectInvalidJsonPayload() throws Exception {
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
            OpenAiTicketReplyDraftProvider provider = newProvider(baseUrl);

            assertThatThrownBy(() -> provider.generateReplyDraft(sampleRequest()))
                    .isInstanceOf(AiProviderException.class)
                    .hasMessage("provider reply draft payload is invalid");
        });
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
