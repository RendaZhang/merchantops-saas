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
public class OpenAiTicketSummaryProvider implements TicketSummaryAiProvider {

    private static final int MAX_OUTPUT_TOKENS = 220;

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;

    @Override
    public TicketSummaryProviderResult generateSummary(TicketSummaryProviderRequest request) {
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

    private Map<String, Object> buildBody(TicketSummaryProviderRequest request) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "summary", Map.of(
                        "type", "string",
                        "description", "A concise ticket summary with the issue, current state, latest signal, and next human follow-up."
                )
        ));
        schema.put("required", List.of("summary"));
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
                                "name", "ticket_summary_response",
                                "strict", true,
                                "schema", schema
                        )
                )
        );
    }

    private TicketSummaryProviderResult parseResponse(TicketSummaryProviderRequest request, JsonNode response) {
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
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider refused the summary request");
        }
        if (!"output_text".equals(contentType)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider returned unsupported content");
        }

        String rawText = contentNode.path("text").asText(null);
        if (!StringUtils.hasText(rawText)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider returned blank content");
        }

        String summary = parseSummary(rawText);
        JsonNode usage = response.path("usage");
        Integer inputTokens = usage.hasNonNull("input_tokens") ? usage.get("input_tokens").asInt() : null;
        Integer outputTokens = usage.hasNonNull("output_tokens") ? usage.get("output_tokens").asInt() : null;
        Integer totalTokens = usage.hasNonNull("total_tokens") ? usage.get("total_tokens").asInt() : null;
        String resolvedModelId = response.path("model").asText(request.modelId());

        return new TicketSummaryProviderResult(
                summary,
                StringUtils.hasText(resolvedModelId) ? resolvedModelId.trim() : request.modelId(),
                inputTokens,
                outputTokens,
                totalTokens,
                null
        );
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

    private String parseSummary(String rawText) {
        try {
            JsonNode payload = objectMapper.readTree(rawText);
            String summary = payload.path("summary").asText(null);
            if (!StringUtils.hasText(summary)) {
                throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider summary payload is blank");
            }
            return summary.trim();
        } catch (JsonProcessingException ex) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider summary payload is invalid");
        }
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
