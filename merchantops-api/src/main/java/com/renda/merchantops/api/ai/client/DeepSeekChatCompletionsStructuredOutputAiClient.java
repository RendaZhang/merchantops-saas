package com.renda.merchantops.api.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.core.AiProviderFailureType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DeepSeekChatCompletionsStructuredOutputAiClient implements StructuredOutputAiClient {

    private final RestClient.Builder restClientBuilder;
    private final AiProperties aiProperties;

    @Override
    public StructuredOutputAiResponse generate(StructuredOutputAiRequest request) {
        try {
            JsonNode response = AiProviderHttpSupport.buildClient(restClientBuilder, aiProperties.resolveBaseUrl(), request.timeoutMs())
                    .post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.resolveApiKey())
                    .header("X-Client-Request-Id", request.requestId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildBody(request))
                    .retrieve()
                    .body(JsonNode.class);
            return parseResponse(request, response);
        } catch (RestClientResponseException ex) {
            AiProviderFailureType failureType = AiProviderHttpSupport.classifyHttpFailure(ex);
            if (failureType == AiProviderFailureType.TIMEOUT) {
                throw new AiProviderException(AiProviderFailureType.TIMEOUT, "ai provider timed out");
            }
            throw new AiProviderException(AiProviderFailureType.UNAVAILABLE, "ai provider returned an error");
        } catch (ResourceAccessException ex) {
            if (AiProviderHttpSupport.isTimeout(ex)) {
                throw new AiProviderException(AiProviderFailureType.TIMEOUT, "ai provider timed out");
            }
            throw new AiProviderException(AiProviderFailureType.UNAVAILABLE, "ai provider is unreachable");
        } catch (RestClientException ex) {
            throw new AiProviderException(AiProviderFailureType.UNAVAILABLE, "ai provider invocation failed");
        }
    }

    private Map<String, Object> buildBody(StructuredOutputAiRequest request) {
        return Map.of(
                "model", request.modelId(),
                "messages", List.of(
                        Map.of("role", "system", "content", buildSystemPrompt(request)),
                        Map.of("role", "user", "content", request.userPrompt())
                ),
                "max_tokens", request.maxOutputTokens(),
                "response_format", Map.of("type", "json_object")
        );
    }

    private String buildSystemPrompt(StructuredOutputAiRequest request) {
        return request.systemPrompt()
                + "\n\nReturn only a valid JSON object."
                + "\nDo not include markdown, code fences, or explanatory prose."
                + "\nUse exactly the same top-level keys as this example shape:"
                + "\n" + request.exampleJson();
    }

    private StructuredOutputAiResponse parseResponse(StructuredOutputAiRequest request, JsonNode response) {
        if (response == null || response.isNull()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider response is empty");
        }

        JsonNode errorNode = response.path("error");
        if (!errorNode.isMissingNode() && !errorNode.isNull()) {
            throw new AiProviderException(AiProviderFailureType.UNAVAILABLE, "provider response reported an error");
        }

        String rawText = AiProviderHttpSupport.extractDeepSeekMessageContent(response);
        JsonNode usage = response.path("usage");
        Integer inputTokens = usage.hasNonNull("prompt_tokens") ? usage.get("prompt_tokens").asInt() : null;
        Integer outputTokens = usage.hasNonNull("completion_tokens") ? usage.get("completion_tokens").asInt() : null;
        Integer totalTokens = usage.hasNonNull("total_tokens") ? usage.get("total_tokens").asInt() : null;
        String resolvedModelId = response.path("model").asText(request.modelId());

        return new StructuredOutputAiResponse(
                rawText,
                resolvedModelId == null ? request.modelId() : resolvedModelId.trim(),
                inputTokens,
                outputTokens,
                totalTokens,
                null
        );
    }
}
