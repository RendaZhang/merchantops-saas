package com.renda.merchantops.api.ai.ticket.summary;

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
public class OpenAiTicketSummaryProvider implements TicketSummaryAiProvider {

    private static final int MAX_OUTPUT_TOKENS = 220;

    private final ObjectMapper objectMapper;
    private final StructuredOutputAiClient structuredOutputAiClient;

    @Override
    public TicketSummaryProviderResult generateSummary(TicketSummaryProviderRequest request) {
        StructuredOutputAiResponse response = structuredOutputAiClient.generate(
                new StructuredOutputAiRequest(
                        request.requestId(),
                        request.modelId(),
                        request.timeoutMs(),
                        request.prompt().systemPrompt(),
                        request.prompt().userPrompt(),
                        MAX_OUTPUT_TOKENS,
                        "ticket_summary_response",
                        buildSchema(),
                        """
                                {
                                  "summary": "Issue: summarize the current ticket facts, current state, latest meaningful signal, and next human follow-up."
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
                        "description", "A concise ticket summary with the issue, current state, latest signal, and next human follow-up."
                )
        ));
        schema.put("required", List.of("summary"));
        schema.put("additionalProperties", false);
        return schema;
    }

    private TicketSummaryProviderResult parseResponse(TicketSummaryProviderRequest request, StructuredOutputAiResponse response) {
        String summary = parseSummary(response.rawText());

        return new TicketSummaryProviderResult(
                summary,
                StringUtils.hasText(response.modelId()) ? response.modelId().trim() : request.modelId(),
                response.inputTokens(),
                response.outputTokens(),
                response.totalTokens(),
                response.costMicros()
        );
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
}
