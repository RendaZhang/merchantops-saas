package com.renda.merchantops.api.ai.importjob.fixrecommendation;

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
public class OpenAiImportJobFixRecommendationProvider implements ImportJobFixRecommendationAiProvider {

    private static final int MAX_OUTPUT_TOKENS = 520;

    private final ObjectMapper objectMapper;
    private final StructuredOutputAiClient structuredOutputAiClient;

    @Override
    public ImportJobFixRecommendationProviderResult generateFixRecommendation(ImportJobFixRecommendationProviderRequest request) {
        StructuredOutputAiResponse response = structuredOutputAiClient.generate(
                new StructuredOutputAiRequest(
                        request.requestId(),
                        request.modelId(),
                        request.timeoutMs(),
                        request.prompt().systemPrompt(),
                        request.prompt().userPrompt(),
                        MAX_OUTPUT_TOKENS,
                        "import_job_fix_recommendation_response",
                        buildSchema(),
                        """
                                {
                                  "summary": "The job is mostly blocked by tenant role validation, with a smaller duplicate-username tail that should be handled as a separate cleanup step.",
                                  "recommendedFixes": [
                                    {
                                      "errorCode": "UNKNOWN_ROLE",
                                      "recommendedAction": "Verify that the referenced role codes exist in the current tenant and normalize the source role-code format before preparing replay input.",
                                      "reasoning": "The grounded failure group points to tenant role validation rather than CSV shape corruption.",
                                      "reviewRequired": true
                                    },
                                    {
                                      "errorCode": "DUPLICATE_USERNAME",
                                      "recommendedAction": "Review the source usernames against current-tenant users and prepare unique replacements before replay.",
                                      "reasoning": "The grounded failure group indicates a uniqueness conflict that needs an operator-reviewed edit.",
                                      "reviewRequired": true
                                    }
                                  ],
                                  "confidenceNotes": [
                                    "The recommendations are grounded in row-level error groups, so operators should still confirm tenant-specific business rules before reuse."
                                  ],
                                  "recommendedOperatorChecks": [
                                    "Confirm which error-code group is the highest-volume cleanup target before editing replay input.",
                                    "Review the affected rows in /errors so value changes can be prepared outside the AI response."
                                  ]
                                }
                                """
                )
        );
        return parseResponse(request, response);
    }

    private Map<String, Object> buildSchema() {
        Map<String, Object> recommendedFixSchema = new LinkedHashMap<>();
        recommendedFixSchema.put("type", "object");
        recommendedFixSchema.put("properties", Map.of(
                "errorCode", Map.of(
                        "type", "string",
                        "description", "Grounded row-level import error code from the current import job."
                ),
                "recommendedAction", Map.of(
                        "type", "string",
                        "description", "Short generic operator action without exposing hidden row values."
                ),
                "reasoning", Map.of(
                        "type", "string",
                        "description", "Why this fix is suggested, grounded in the sanitized row-level failure group."
                ),
                "reviewRequired", Map.of(
                        "type", "boolean",
                        "description", "Whether an operator should manually confirm this fix before downstream use."
                )
        ));
        recommendedFixSchema.put("required", List.of("errorCode", "recommendedAction", "reasoning", "reviewRequired"));
        recommendedFixSchema.put("additionalProperties", false);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "summary", Map.of(
                        "type", "string",
                        "description", "A concise summary grounded in the sanitized import-job facts."
                ),
                "recommendedFixes", Map.of(
                        "type", "array",
                        "description", "Fix recommendations keyed by grounded row-level import error code.",
                        "items", recommendedFixSchema
                ),
                "confidenceNotes", Map.of(
                        "type", "array",
                        "description", "Notes about ambiguity, missing signal, or review risk.",
                        "items", Map.of("type", "string")
                ),
                "recommendedOperatorChecks", Map.of(
                        "type", "array",
                        "description", "Concrete operator checks before preparing replay input.",
                        "items", Map.of("type", "string")
                )
        ));
        schema.put("required", List.of("summary", "recommendedFixes", "confidenceNotes", "recommendedOperatorChecks"));
        schema.put("additionalProperties", false);
        return schema;
    }

    private ImportJobFixRecommendationProviderResult parseResponse(ImportJobFixRecommendationProviderRequest request,
                                                                  StructuredOutputAiResponse response) {
        JsonNode payload = parsePayload(response.rawText());
        String summary = readRequiredText(payload, "summary");
        List<ImportJobFixRecommendationProviderResult.RecommendedFix> recommendedFixes =
                readRequiredFixes(payload, "recommendedFixes");
        List<String> confidenceNotes = readRequiredStringArray(payload, "confidenceNotes");
        List<String> recommendedOperatorChecks = readRequiredStringArray(payload, "recommendedOperatorChecks");

        return new ImportJobFixRecommendationProviderResult(
                summary,
                recommendedFixes,
                confidenceNotes,
                recommendedOperatorChecks,
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
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import fix recommendation payload is invalid");
        }
    }

    private String readRequiredText(JsonNode payload, String fieldName) {
        String value = payload.path(fieldName).asText(null);
        if (!StringUtils.hasText(value)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import fix recommendation payload is missing " + fieldName);
        }
        return value.trim();
    }

    private List<String> readRequiredStringArray(JsonNode payload, String fieldName) {
        JsonNode arrayNode = payload.path(fieldName);
        if (!arrayNode.isArray()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import fix recommendation payload is missing " + fieldName);
        }
        List<String> items = new ArrayList<>();
        arrayNode.forEach(node -> {
            String value = node.asText(null);
            if (!StringUtils.hasText(value)) {
                throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import fix recommendation payload has blank " + fieldName + " item");
            }
            items.add(value.trim());
        });
        if (items.isEmpty()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import fix recommendation payload has empty " + fieldName);
        }
        return items;
    }

    private List<ImportJobFixRecommendationProviderResult.RecommendedFix> readRequiredFixes(JsonNode payload, String fieldName) {
        JsonNode arrayNode = payload.path(fieldName);
        if (!arrayNode.isArray()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import fix recommendation payload is missing " + fieldName);
        }
        List<ImportJobFixRecommendationProviderResult.RecommendedFix> items = new ArrayList<>();
        arrayNode.forEach(node -> items.add(readRecommendedFix(node)));
        if (items.isEmpty()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import fix recommendation payload has empty " + fieldName);
        }
        return items;
    }

    private ImportJobFixRecommendationProviderResult.RecommendedFix readRecommendedFix(JsonNode node) {
        if (!node.isObject()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import fix recommendation payload has invalid recommendedFixes item");
        }
        return new ImportJobFixRecommendationProviderResult.RecommendedFix(
                readRequiredText(node, "errorCode"),
                readRequiredText(node, "recommendedAction"),
                readRequiredText(node, "reasoning"),
                readRequiredBoolean(node, "reviewRequired")
        );
    }

    private boolean readRequiredBoolean(JsonNode payload, String fieldName) {
        JsonNode node = payload.path(fieldName);
        if (!node.isBoolean()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import fix recommendation payload has invalid " + fieldName);
        }
        return node.asBoolean();
    }
}
