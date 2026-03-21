package com.renda.merchantops.api.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiTicketReplyDraftProvider implements TicketReplyDraftAiProvider {

    private static final int MAX_OUTPUT_TOKENS = 320;

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;

    @Override
    public TicketReplyDraftProviderResult generateReplyDraft(TicketReplyDraftProviderRequest request) {
        try {
            JsonNode response = buildClient(request.timeoutMs())
                    .post()
                    .uri("/v1/responses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getOpenai().getApiKey().trim())
                    .header("X-Client-Request-Id", request.requestId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildBody(request))
                    .retrieve()
                    .body(JsonNode.class);
            return parseResponse(request, response);
        } catch (RestClientResponseException ex) {
            throw new AiProviderException(AiProviderFailureType.UNAVAILABLE, "ai provider returned an error");
        } catch (ResourceAccessException ex) {
            if (isTimeout(ex)) {
                throw new AiProviderException(AiProviderFailureType.TIMEOUT, "ai provider timed out");
            }
            throw new AiProviderException(AiProviderFailureType.UNAVAILABLE, "ai provider is unreachable");
        } catch (RestClientException ex) {
            throw new AiProviderException(AiProviderFailureType.UNAVAILABLE, "ai provider invocation failed");
        }
    }

    private RestClient buildClient(int timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        return restClientBuilder
                .baseUrl(normalizeBaseUrl(aiProperties.getOpenai().getBaseUrl()))
                .requestFactory(requestFactory)
                .build();
    }

    private Map<String, Object> buildBody(TicketReplyDraftProviderRequest request) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "opening", Map.of(
                        "type", "string",
                        "description", "A concise opening sentence for an internal ticket comment."
                ),
                "body", Map.of(
                        "type", "string",
                        "description", "The main ticket-context body for an internal operator comment."
                ),
                "nextStep", Map.of(
                        "type", "string",
                        "description", "The next practical human follow-up."
                ),
                "closing", Map.of(
                        "type", "string",
                        "description", "A short internal closing sentence."
                )
        ));
        schema.put("required", List.of("opening", "body", "nextStep", "closing"));
        schema.put("additionalProperties", false);

        return Map.of(
                "model", request.modelId(),
                "input", List.of(
                        Map.of("role", "system", "content", request.prompt().systemPrompt()),
                        Map.of("role", "user", "content", request.prompt().userPrompt())
                ),
                "max_output_tokens", MAX_OUTPUT_TOKENS,
                "text", Map.of(
                        "format", Map.of(
                                "type", "json_schema",
                                "name", "ticket_reply_draft_response",
                                "strict", true,
                                "schema", schema
                        )
                )
        );
    }

    private TicketReplyDraftProviderResult parseResponse(TicketReplyDraftProviderRequest request, JsonNode response) {
        if (response == null || response.isNull()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider response is empty");
        }

        JsonNode errorNode = response.path("error");
        if (!errorNode.isMissingNode() && !errorNode.isNull()) {
            throw new AiProviderException(AiProviderFailureType.UNAVAILABLE, "provider response reported an error");
        }

        JsonNode contentNode = findFirstContentNode(response.path("output"));
        if (contentNode == null) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider response did not include content");
        }

        String contentType = contentNode.path("type").asText();
        if ("refusal".equals(contentType)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider refused the reply draft request");
        }
        if (!"output_text".equals(contentType)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider returned unsupported content");
        }

        String rawText = contentNode.path("text").asText(null);
        if (!StringUtils.hasText(rawText)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider returned blank content");
        }

        JsonNode payload = parsePayload(rawText);
        String opening = readRequiredText(payload, "opening");
        String body = readRequiredText(payload, "body");
        String nextStep = readRequiredText(payload, "nextStep");
        String closing = readRequiredText(payload, "closing");
        JsonNode usage = response.path("usage");
        Integer inputTokens = usage.hasNonNull("input_tokens") ? usage.get("input_tokens").asInt() : null;
        Integer outputTokens = usage.hasNonNull("output_tokens") ? usage.get("output_tokens").asInt() : null;
        Integer totalTokens = usage.hasNonNull("total_tokens") ? usage.get("total_tokens").asInt() : null;
        String resolvedModelId = response.path("model").asText(request.modelId());

        return new TicketReplyDraftProviderResult(
                opening,
                body,
                nextStep,
                closing,
                StringUtils.hasText(resolvedModelId) ? resolvedModelId.trim() : request.modelId(),
                inputTokens,
                outputTokens,
                totalTokens,
                null
        );
    }

    private JsonNode parsePayload(String rawText) {
        try {
            return objectMapper.readTree(rawText);
        } catch (JsonProcessingException ex) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider reply draft payload is invalid");
        }
    }

    private String readRequiredText(JsonNode payload, String fieldName) {
        String value = payload.path(fieldName).asText(null);
        if (!StringUtils.hasText(value)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider reply draft payload is missing " + fieldName);
        }
        return value.trim();
    }

    private JsonNode findFirstContentNode(JsonNode outputNode) {
        if (outputNode == null || !outputNode.isArray()) {
            return null;
        }
        for (JsonNode outputItem : outputNode) {
            JsonNode content = outputItem.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                return contentItem;
            }
        }
        return null;
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String normalizeBaseUrl(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
