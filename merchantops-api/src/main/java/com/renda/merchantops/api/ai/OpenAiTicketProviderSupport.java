package com.renda.merchantops.api.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;

final class OpenAiTicketProviderSupport {

    private OpenAiTicketProviderSupport() {
    }

    static OutputTextExtraction extractOutputText(JsonNode outputNode) {
        if (outputNode == null || !outputNode.isArray()) {
            return new OutputTextExtraction(OutputTextExtractionStatus.MISSING_CONTENT, null);
        }

        boolean sawContentArray = false;
        boolean sawContentItem = false;
        boolean sawRefusal = false;
        boolean sawOutputText = false;
        StringBuilder textBuilder = new StringBuilder();

        for (JsonNode outputItem : outputNode) {
            JsonNode content = outputItem.path("content");
            if (!content.isArray()) {
                continue;
            }
            sawContentArray = true;
            for (JsonNode contentItem : content) {
                sawContentItem = true;
                String contentType = contentItem.path("type").asText();
                if ("refusal".equals(contentType)) {
                    sawRefusal = true;
                    continue;
                }
                if (!"output_text".equals(contentType)) {
                    continue;
                }
                sawOutputText = true;
                String text = contentItem.path("text").asText(null);
                if (StringUtils.hasText(text)) {
                    textBuilder.append(text);
                }
            }
        }

        if (StringUtils.hasText(textBuilder.toString())) {
            return new OutputTextExtraction(OutputTextExtractionStatus.OUTPUT_TEXT, textBuilder.toString());
        }
        if (!sawContentArray || !sawContentItem) {
            return new OutputTextExtraction(OutputTextExtractionStatus.MISSING_CONTENT, null);
        }
        if (sawOutputText) {
            return new OutputTextExtraction(OutputTextExtractionStatus.BLANK_OUTPUT_TEXT, null);
        }
        if (sawRefusal) {
            return new OutputTextExtraction(OutputTextExtractionStatus.REFUSAL_ONLY, null);
        }
        return new OutputTextExtraction(OutputTextExtractionStatus.UNSUPPORTED_CONTENT, null);
    }

    static AiProviderFailureType classifyHttpFailure(RestClientResponseException ex) {
        int statusCode = ex.getStatusCode().value();
        if (statusCode == 408 || statusCode == 504) {
            return AiProviderFailureType.TIMEOUT;
        }
        return AiProviderFailureType.UNAVAILABLE;
    }

    record OutputTextExtraction(
            OutputTextExtractionStatus status,
            String text
    ) {
    }

    enum OutputTextExtractionStatus {
        OUTPUT_TEXT,
        MISSING_CONTENT,
        BLANK_OUTPUT_TEXT,
        REFUSAL_ONLY,
        UNSUPPORTED_CONTENT
    }
}
