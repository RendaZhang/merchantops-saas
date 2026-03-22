package com.renda.merchantops.api.ai;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

final class OpenAiFixtureServer {

    private OpenAiFixtureServer() {
    }

    static void withServer(int statusCode,
                           String responseBody,
                           ThrowingConsumer<ServerHandle> consumer) throws Exception {
        withServer("/v1/responses", statusCode, responseBody, consumer);
    }

    static void withServer(String contextPath,
                           int statusCode,
                           String responseBody,
                           ThrowingConsumer<ServerHandle> consumer) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        AtomicReference<CapturedRequest> capturedRequest = new AtomicReference<>();
        server.createContext(contextPath, exchange -> {
            byte[] requestBytes = exchange.getRequestBody().readAllBytes();
            capturedRequest.set(new CapturedRequest(
                    exchange.getRequestMethod(),
                    copyHeaders(exchange.getRequestHeaders()),
                    new String(requestBytes, StandardCharsets.UTF_8)
            ));

            byte[] responseBytes = responseBody == null
                    ? new byte[0]
                    : responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBytes);
            }
        });
        server.start();
        try {
            consumer.accept(new ServerHandle("http://127.0.0.1:" + server.getAddress().getPort(), capturedRequest));
        } finally {
            server.stop(0);
        }
    }

    private static Map<String, List<String>> copyHeaders(Headers headers) {
        return headers.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    record ServerHandle(
            String baseUrl,
            AtomicReference<CapturedRequest> capturedRequest
    ) {
        CapturedRequest requireCapturedRequest() {
            return Objects.requireNonNull(capturedRequest.get(), "expected provider request to be captured");
        }
    }

    record CapturedRequest(
            String method,
            Map<String, List<String>> headers,
            String body
    ) {
        String header(String name) {
            return headers.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                    .findFirst()
                    .map(entry -> entry.getValue().isEmpty() ? null : entry.getValue().getFirst())
                    .orElse(null);
        }
    }

    @FunctionalInterface
    interface ThrowingConsumer<T> {
        void accept(T value) throws Exception;
    }
}
