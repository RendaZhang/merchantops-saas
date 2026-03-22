package com.renda.merchantops.api.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;

final class AiProviderHttpSupport {

    private AiProviderHttpSupport() {
    }

    static RestClient buildClient(RestClient.Builder restClientBuilder, String baseUrl, int timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        return restClientBuilder
                .baseUrl(normalizeBaseUrl(baseUrl))
                .requestFactory(requestFactory)
                .build();
    }

    static AiProviderFailureType classifyHttpFailure(RestClientResponseException ex) {
        int statusCode = ex.getStatusCode().value();
        if (statusCode == 408 || statusCode == 504) {
            return AiProviderFailureType.TIMEOUT;
        }
        return AiProviderFailureType.UNAVAILABLE;
    }

    static boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    static String normalizeBaseUrl(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    static String toUnavailableMessage(Throwable throwable, String defaultMessage) {
        if (throwable instanceof ResourceAccessException && isTimeout(throwable)) {
            return "ai provider timed out";
        }
        return defaultMessage;
    }

    static OutputTextExtraction extractOpenAiOutputText(JsonNode outputNode) {
        if (outputNode == null || !outputNode.isArray()) {
            return new OutputTextExtraction(OutputTextExtractionStatus.MISSING_CONTENT, null);
        }

        boolean sawContentArray = false;
        boolean sawContentItem = false;
        boolean sawRefusal = false;
        boolean sawOutputText = false;
        StringBuilder textBuilder = new StringBuilder();

        for (JsonNode outputItem : outputNode) {
            JsonNode content = outputItem.path("content");
            if (!content.isArray()) {
                continue;
            }
            sawContentArray = true;
            for (JsonNode contentItem : content) {
                sawContentItem = true;
                String contentType = contentItem.path("type").asText();
                if ("refusal".equals(contentType)) {
                    sawRefusal = true;
                    continue;
                }
                if (!"output_text".equals(contentType)) {
                    continue;
                }
                sawOutputText = true;
                String text = contentItem.path("text").asText(null);
                if (StringUtils.hasText(text)) {
                    textBuilder.append(text);
                }
            }
        }

        if (StringUtils.hasText(textBuilder.toString())) {
            return new OutputTextExtraction(OutputTextExtractionStatus.OUTPUT_TEXT, textBuilder.toString());
        }
        if (!sawContentArray || !sawContentItem) {
            return new OutputTextExtraction(OutputTextExtractionStatus.MISSING_CONTENT, null);
        }
        if (sawOutputText) {
            return new OutputTextExtraction(OutputTextExtractionStatus.BLANK_OUTPUT_TEXT, null);
        }
        if (sawRefusal) {
            return new OutputTextExtraction(OutputTextExtractionStatus.REFUSAL_ONLY, null);
        }
        return new OutputTextExtraction(OutputTextExtractionStatus.UNSUPPORTED_CONTENT, null);
    }

    static String extractDeepSeekMessageContent(JsonNode response) {
        JsonNode choices = response.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider response did not include choices");
        }
        JsonNode firstChoice = choices.get(0);
        JsonNode message = firstChoice.path("message");
        String content = message.path("content").asText(null);
        if (!StringUtils.hasText(content)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider returned blank content");
        }
        return content.trim();
    }

    record OutputTextExtraction(
            OutputTextExtractionStatus status,
            String text
    ) {
    }

    enum OutputTextExtractionStatus {
        OUTPUT_TEXT,
        MISSING_CONTENT,
        BLANK_OUTPUT_TEXT,
        REFUSAL_ONLY,
        UNSUPPORTED_CONTENT
    }
}
