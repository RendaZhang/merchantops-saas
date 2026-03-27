package com.renda.merchantops.api.ai.importjob.mappingsuggestion;

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
public class OpenAiImportJobMappingSuggestionProvider implements ImportJobMappingSuggestionAiProvider {

    private static final int MAX_OUTPUT_TOKENS = 520;

    private final ObjectMapper objectMapper;
    private final StructuredOutputAiClient structuredOutputAiClient;

    @Override
    public ImportJobMappingSuggestionProviderResult generateMappingSuggestion(ImportJobMappingSuggestionProviderRequest request) {
        StructuredOutputAiResponse response = structuredOutputAiClient.generate(
                new StructuredOutputAiRequest(
                        request.requestId(),
                        request.modelId(),
                        request.timeoutMs(),
                        request.prompt().systemPrompt(),
                        request.prompt().userPrompt(),
                        MAX_OUTPUT_TOKENS,
                        "import_job_mapping_suggestion_response",
                        buildSchema(),
                        """
                                {
                                  "summary": "The failed header still looks close to the canonical USER_CSV schema, so the safest next step is to confirm the proposed field mappings before preparing any replay input.",
                                  "suggestedFieldMappings": [
                                    {
                                      "canonicalField": "username",
                                      "observedColumnSignal": {
                                        "headerName": "login",
                                        "headerPosition": 1
                                      },
                                      "reasoning": "`login` is the closest observed header for the canonical username field.",
                                      "reviewRequired": false
                                    },
                                    {
                                      "canonicalField": "displayName",
                                      "observedColumnSignal": {
                                        "headerName": "display_name",
                                        "headerPosition": 2
                                      },
                                      "reasoning": "`display_name` is the closest semantic match for displayName.",
                                      "reviewRequired": false
                                    },
                                    {
                                      "canonicalField": "email",
                                      "observedColumnSignal": {
                                        "headerName": "email_address",
                                        "headerPosition": 3
                                      },
                                      "reasoning": "`email_address` is the most likely email column.",
                                      "reviewRequired": false
                                    },
                                    {
                                      "canonicalField": "password",
                                      "observedColumnSignal": {
                                        "headerName": "passwd",
                                        "headerPosition": 4
                                      },
                                      "reasoning": "`passwd` is the strongest credential-like header, but the tenant should still confirm it before downstream use.",
                                      "reviewRequired": true
                                    },
                                    {
                                      "canonicalField": "roleCodes",
                                      "observedColumnSignal": {
                                        "headerName": "roles",
                                        "headerPosition": 5
                                      },
                                      "reasoning": "`roles` is the closest available signal for the canonical roleCodes field.",
                                      "reviewRequired": true
                                    }
                                  ],
                                  "confidenceNotes": [
                                    "The source file failed header validation, so each suggested mapping should be reviewed before reuse."
                                  ],
                                  "recommendedOperatorChecks": [
                                    "Confirm the source header order before editing any replay input.",
                                    "Verify that the observed `roles` column really contains tenant role codes in the expected delimiter format."
                                  ]
                                }
                                """
                )
        );
        return parseResponse(request, response);
    }

    private Map<String, Object> buildSchema() {
        Map<String, Object> observedColumnSignalSchema = new LinkedHashMap<>();
        observedColumnSignalSchema.put("type", "object");
        observedColumnSignalSchema.put("properties", Map.of(
                "headerName", Map.of(
                        "type", "string",
                        "description", "Normalized header token extracted from the sanitized header signal."
                ),
                "headerPosition", Map.of(
                        "type", "integer",
                        "description", "1-based header position."
                )
        ));
        observedColumnSignalSchema.put("required", List.of("headerName", "headerPosition"));
        observedColumnSignalSchema.put("additionalProperties", false);

        Map<String, Object> suggestedFieldMappingSchema = new LinkedHashMap<>();
        suggestedFieldMappingSchema.put("type", "object");
        suggestedFieldMappingSchema.put("properties", Map.of(
                "canonicalField", Map.of(
                        "type", "string",
                        "description", "Canonical USER_CSV field name."
                ),
                "observedColumnSignal", Map.of(
                        "type", List.of("object", "null"),
                        "properties", observedColumnSignalSchema.get("properties"),
                        "required", observedColumnSignalSchema.get("required"),
                        "additionalProperties", false,
                        "description", "Suggested observed source-column signal, or null when no safe single-column match exists."
                ),
                "reasoning", Map.of(
                        "type", "string",
                        "description", "Why this mapping is suggested or why manual review remains required."
                ),
                "reviewRequired", Map.of(
                        "type", "boolean",
                        "description", "Whether an operator should manually confirm this mapping."
                )
        ));
        suggestedFieldMappingSchema.put("required", List.of("canonicalField", "observedColumnSignal", "reasoning", "reviewRequired"));
        suggestedFieldMappingSchema.put("additionalProperties", false);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "summary", Map.of(
                        "type", "string",
                        "description", "A concise summary grounded in the sanitized import-job facts."
                ),
                "suggestedFieldMappings", Map.of(
                        "type", "array",
                        "description", "Suggested canonical-field mappings extracted from the sanitized header signal.",
                        "items", suggestedFieldMappingSchema
                ),
                "confidenceNotes", Map.of(
                        "type", "array",
                        "description", "Notes about ambiguity, missing signal, or review risk.",
                        "items", Map.of("type", "string")
                ),
                "recommendedOperatorChecks", Map.of(
                        "type", "array",
                        "description", "Concrete operator checks before preparing any replay input.",
                        "items", Map.of("type", "string")
                )
        ));
        schema.put("required", List.of("summary", "suggestedFieldMappings", "confidenceNotes", "recommendedOperatorChecks"));
        schema.put("additionalProperties", false);
        return schema;
    }

    private ImportJobMappingSuggestionProviderResult parseResponse(ImportJobMappingSuggestionProviderRequest request,
                                                                  StructuredOutputAiResponse response) {
        JsonNode payload = parsePayload(response.rawText());
        String summary = readRequiredText(payload, "summary");
        List<ImportJobMappingSuggestionProviderResult.SuggestedFieldMapping> suggestedFieldMappings =
                readRequiredMappings(payload, "suggestedFieldMappings");
        List<String> confidenceNotes = readRequiredStringArray(payload, "confidenceNotes");
        List<String> recommendedOperatorChecks = readRequiredStringArray(payload, "recommendedOperatorChecks");

        return new ImportJobMappingSuggestionProviderResult(
                summary,
                suggestedFieldMappings,
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
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload is invalid");
        }
    }

    private String readRequiredText(JsonNode payload, String fieldName) {
        String value = payload.path(fieldName).asText(null);
        if (!StringUtils.hasText(value)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload is missing " + fieldName);
        }
        return value.trim();
    }

    private List<String> readRequiredStringArray(JsonNode payload, String fieldName) {
        JsonNode arrayNode = payload.path(fieldName);
        if (!arrayNode.isArray()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload is missing " + fieldName);
        }
        List<String> items = new ArrayList<>();
        arrayNode.forEach(node -> {
            String value = node.asText(null);
            if (!StringUtils.hasText(value)) {
                throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload has blank " + fieldName + " item");
            }
            items.add(value.trim());
        });
        if (items.isEmpty()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload has empty " + fieldName);
        }
        return items;
    }

    private List<ImportJobMappingSuggestionProviderResult.SuggestedFieldMapping> readRequiredMappings(JsonNode payload, String fieldName) {
        JsonNode arrayNode = payload.path(fieldName);
        if (!arrayNode.isArray()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload is missing " + fieldName);
        }
        List<ImportJobMappingSuggestionProviderResult.SuggestedFieldMapping> items = new ArrayList<>();
        arrayNode.forEach(node -> items.add(readSuggestedFieldMapping(node)));
        if (items.isEmpty()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload has empty " + fieldName);
        }
        return items;
    }

    private ImportJobMappingSuggestionProviderResult.SuggestedFieldMapping readSuggestedFieldMapping(JsonNode node) {
        if (!node.isObject()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload has invalid suggestedFieldMappings item");
        }
        if (!node.has("observedColumnSignal")) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload is missing suggestedFieldMappings.observedColumnSignal");
        }
        return new ImportJobMappingSuggestionProviderResult.SuggestedFieldMapping(
                readRequiredText(node, "canonicalField"),
                readObservedColumnSignal(node.get("observedColumnSignal")),
                readRequiredText(node, "reasoning"),
                readRequiredBoolean(node, "reviewRequired")
        );
    }

    private ImportJobMappingSuggestionProviderResult.ObservedColumnSignal readObservedColumnSignal(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isObject()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload has invalid observedColumnSignal");
        }
        String headerName = readRequiredText(node, "headerName");
        Integer headerPosition = readRequiredPositiveInteger(node, "headerPosition");
        return new ImportJobMappingSuggestionProviderResult.ObservedColumnSignal(headerName, headerPosition);
    }

    private Integer readRequiredPositiveInteger(JsonNode payload, String fieldName) {
        JsonNode node = payload.path(fieldName);
        if (!node.isIntegralNumber() || node.asInt() < 1) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload has invalid " + fieldName);
        }
        return node.asInt();
    }

    private boolean readRequiredBoolean(JsonNode payload, String fieldName) {
        JsonNode node = payload.path(fieldName);
        if (!node.isBoolean()) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider import mapping suggestion payload has invalid " + fieldName);
        }
        return node.asBoolean();
    }
}
