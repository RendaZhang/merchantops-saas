package com.renda.merchantops.api.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.dto.ticket.query.TicketAiTriagePriority;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiTicketTriageProvider implements TicketTriageAiProvider {

    private static final int MAX_OUTPUT_TOKENS = 220;

    private final ObjectMapper objectMapper;
    private final StructuredOutputAiClient structuredOutputAiClient;

    @Override
    public TicketTriageProviderResult generateTriage(TicketTriageProviderRequest request) {
        StructuredOutputAiResponse response = structuredOutputAiClient.generate(
                new StructuredOutputAiRequest(
                        request.requestId(),
                        request.modelId(),
                        request.timeoutMs(),
                        request.prompt().systemPrompt(),
                        request.prompt().userPrompt(),
                        MAX_OUTPUT_TOKENS,
                        "ticket_triage_response",
                        buildSchema(),
                        """
                                {
                                  "classification": "DEVICE_ISSUE",
                                  "priority": "HIGH",
                                  "reasoning": "Briefly explain the priority and classification from the ticket facts."
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
                "classification", Map.of(
                        "type", "string",
                        "description", "A short ticket issue label."
                ),
                "priority", Map.of(
                        "type", "string",
                        "enum", List.of("LOW", "MEDIUM", "HIGH"),
                        "description", "The suggested ticket priority."
                ),
                "reasoning", Map.of(
                        "type", "string",
                        "description", "A short human-readable explanation grounded in the ticket facts."
                )
        ));
        schema.put("required", List.of("classification", "priority", "reasoning"));
        schema.put("additionalProperties", false);
        return schema;
    }

    private TicketTriageProviderResult parseResponse(TicketTriageProviderRequest request, StructuredOutputAiResponse response) {
        JsonNode payload = parsePayload(response.rawText());
        String classification = readRequiredText(payload, "classification");
        String reasoning = readRequiredText(payload, "reasoning");
        TicketAiTriagePriority priority = parsePriority(payload.path("priority").asText(null));

        return new TicketTriageProviderResult(
                classification,
                priority,
                reasoning,
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
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider triage payload is invalid");
        }
    }

    private String readRequiredText(JsonNode payload, String fieldName) {
        String value = payload.path(fieldName).asText(null);
        if (!StringUtils.hasText(value)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider triage payload is missing " + fieldName);
        }
        return value.trim();
    }

    private TicketAiTriagePriority parsePriority(String value) {
        if (!StringUtils.hasText(value)) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider triage payload is missing priority");
        }
        try {
            return TicketAiTriagePriority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new AiProviderException(AiProviderFailureType.INVALID_RESPONSE, "provider triage payload has invalid priority");
        }
    }
}
