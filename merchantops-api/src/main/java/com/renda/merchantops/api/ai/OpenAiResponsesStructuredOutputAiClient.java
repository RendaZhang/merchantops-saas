package com.renda.merchantops.api.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.renda.merchantops.api.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiResponsesStructuredOutputAiClient implements StructuredOutputAiClient {

    private final RestClient.Builder restClientBuilder;
    private final AiProperties aiProperties;

    @Override
    public StructuredOutputAiResponse generate(StructuredOutputAiRequest request) {
        try {
            JsonNode response = AiProviderHttpSupport.buildClient(restClientBuilder, aiProperties.resolveBaseUrl(), request.timeoutMs())
                    .post()
                    .uri("/v1/responses")
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
                "input", List.of(
                        Map.of("role", "system", "content", request.systemPrompt()),
                        Map.of("role", "user", "content", request.userPrompt())
                ),
                "max_output_tokens", request.maxOutputTokens(),
                "text", Map.of(
                        "format", Map.of(
                                "type", "json_schema",
                                "name", request.schemaName(),
                                "strict", true,
                                "schema", request.schema()
                        )
                )
        );
    }

    private StructuredOutputAiResponse parseResponse(StructuredOutputAiRequest request, JsonNode response) {
        if (response == null || response.isNull()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider response is empty");
        }

        JsonNode errorNode = response.path("error");
        if (!errorNode.isMissingNode() && !errorNode.isNull()) {
            throw new AiProviderException(AiProviderFailureType.UNAVAILABLE, "provider response reported an error");
        }

        AiProviderHttpSupport.OutputTextExtraction extraction = AiProviderHttpSupport.extractOpenAiOutputText(response.path("output"));
        String rawText = switch (extraction.status()) {
            case OUTPUT_TEXT -> extraction.text();
            case MISSING_CONTENT ->
                    throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider response did not include content");
            case BLANK_OUTPUT_TEXT ->
                    throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider returned blank content");
            case REFUSAL_ONLY ->
                    throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider refused the request");
            case UNSUPPORTED_CONTENT ->
                    throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider returned unsupported content");
        };
        if (!StringUtils.hasText(rawText)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider returned blank content");
        }

        JsonNode usage = response.path("usage");
        Integer inputTokens = usage.hasNonNull("input_tokens") ? usage.get("input_tokens").asInt() : null;
        Integer outputTokens = usage.hasNonNull("output_tokens") ? usage.get("output_tokens").asInt() : null;
        Integer totalTokens = usage.hasNonNull("total_tokens") ? usage.get("total_tokens").asInt() : null;
        String resolvedModelId = response.path("model").asText(request.modelId());

        return new StructuredOutputAiResponse(
                rawText,
                StringUtils.hasText(resolvedModelId) ? resolvedModelId.trim() : request.modelId(),
                inputTokens,
                outputTokens,
                totalTokens,
                null
        );
    }
}
