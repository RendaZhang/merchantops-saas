package com.renda.merchantops.api.ai.importjob.errorsummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.ai.client.StructuredOutputAiClient;
import com.renda.merchantops.api.ai.client.StructuredOutputAiRequest;
import com.renda.merchantops.api.ai.client.StructuredOutputAiResponse;
import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.core.AiProviderFailureType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiImportJobErrorSummaryProvider implements ImportJobErrorSummaryAiProvider {

    private static final int MAX_OUTPUT_TOKENS = 360;

    private final ObjectMapper objectMapper;
    private final StructuredOutputAiClient structuredOutputAiClient;

    @Override
    public ImportJobErrorSummaryProviderResult generateErrorSummary(ImportJobErrorSummaryProviderRequest request) {
        StructuredOutputAiResponse response = structuredOutputAiClient.generate(
                new StructuredOutputAiRequest(
                        request.requestId(),
                        request.modelId(),
                        request.timeoutMs(),
                        request.prompt().systemPrompt(),
                        request.prompt().userPrompt(),
                        MAX_OUTPUT_TOKENS,
                        "import_job_error_summary_response",
                        buildSchema(),
                        """
                                {
                                  "summary": "The job is dominated by role validation failures with structurally complete rows, so the next manual step is to correct role mappings before retrying.",
                                  "topErrorPatterns": [
                                    "UNKNOWN_ROLE appears on multiple rows where roleCodes is present but invalid for the current tenant.",
                                    "Most failing rows are structurally complete, which suggests business validation issues rather than CSV shape problems."
                                  ],
                                  "recommendedNextSteps": [
                                    "Review the current tenant role catalog and correct invalid roleCodes before replaying failures.",
                                    "Use the import job detail and /errors page to confirm which rows need edited replay versus selective replay."
                                  ]
                                }
                                """
                )
        );
        return parseResponse(request, response);
    }

    private Map<String, Object> buildSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "summary", Map.of(
                        "type", "string",
                        "description", "A concise summary grounded in the import job facts and sanitized failure context."
                ),
                "topErrorPatterns", Map.of(
                        "type", "array",
                        "description", "Concrete error patterns observed in the import job.",
                        "items", Map.of("type", "string")
                ),
                "recommendedNextSteps", Map.of(
                        "type", "array",
                        "description", "Concrete next manual steps before retrying or replaying rows.",
                        "items", Map.of("type", "string")
                )
        ));
        schema.put("required", List.of("summary", "topErrorPatterns", "recommendedNextSteps"));
        schema.put("additionalProperties", false);
        return schema;
    }

    private ImportJobErrorSummaryProviderResult parseResponse(ImportJobErrorSummaryProviderRequest request,
                                                              StructuredOutputAiResponse response) {
        JsonNode payload = parsePayload(response.rawText());
        String summary = readRequiredText(payload, "summary");
        List<String> topErrorPatterns = readRequiredStringArray(payload, "topErrorPatterns");
        List<String> recommendedNextSteps = readRequiredStringArray(payload, "recommendedNextSteps");

        return new ImportJobErrorSummaryProviderResult(
                summary,
                topErrorPatterns,
                recommendedNextSteps,
                StringUtils.hasText(response.modelId()) ? response.modelId().trim() : request.modelId(),
                response.inputTokens(),
                response.outputTokens(),
                response.totalTokens(),
                response.costMicros()
        );
    }

    private JsonNode parsePayload(String rawText) {
        try {
            return objectMapper.readTree(rawText);
        } catch (JsonProcessingException ex) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import error summary payload is invalid");
        }
    }

    private String readRequiredText(JsonNode payload, String fieldName) {
        String value = payload.path(fieldName).asText(null);
        if (!StringUtils.hasText(value)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import error summary payload is missing " + fieldName);
        }
        return value.trim();
    }

    private List<String> readRequiredStringArray(JsonNode payload, String fieldName) {
        JsonNode arrayNode = payload.path(fieldName);
        if (!arrayNode.isArray()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import error summary payload is missing " + fieldName);
        }
        List<String> items = new ArrayList<>();
        arrayNode.forEach(node -> {
            String value = node.asText(null);
            if (!StringUtils.hasText(value)) {
                throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import error summary payload has blank " + fieldName + " item");
            }
            items.add(value.trim());
        });
        return items;
    }
}
