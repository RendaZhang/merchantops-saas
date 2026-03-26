package com.renda.merchantops.api.ai.ticket.replydraft;

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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiTicketReplyDraftProvider implements TicketReplyDraftAiProvider {

    private static final int MAX_OUTPUT_TOKENS = 320;

    private final ObjectMapper objectMapper;
    private final StructuredOutputAiClient structuredOutputAiClient;

    @Override
    public TicketReplyDraftProviderResult generateReplyDraft(TicketReplyDraftProviderRequest request) {
        StructuredOutputAiResponse response = structuredOutputAiClient.generate(
                new StructuredOutputAiRequest(
                        request.requestId(),
                        request.modelId(),
                        request.timeoutMs(),
                        request.prompt().systemPrompt(),
                        request.prompt().userPrompt(),
                        MAX_OUTPUT_TOKENS,
                        "ticket_reply_draft_response",
                        buildSchema(),
                        """
                                {
                                  "opening": "Quick internal update.",
                                  "body": "State the current ticket facts without inventing actions.",
                                  "nextStep": "Describe the next human follow-up.",
                                  "closing": "Close with a short internal sentence."
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
        return schema;
    }

    private TicketReplyDraftProviderResult parseResponse(TicketReplyDraftProviderRequest request, StructuredOutputAiResponse response) {
        JsonNode payload = parsePayload(response.rawText());
        String opening = readRequiredText(payload, "opening");
        String body = readRequiredText(payload, "body");
        String nextStep = readRequiredText(payload, "nextStep");
        String closing = readRequiredText(payload, "closing");

        return new TicketReplyDraftProviderResult(
                opening,
                body,
                nextStep,
                closing,
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
}
