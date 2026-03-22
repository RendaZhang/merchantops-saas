package com.renda.merchantops.api.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiTicketSummaryProviderTest {

    private final StubStructuredOutputAiClient structuredOutputAiClient = new StubStructuredOutputAiClient();
    private final OpenAiTicketSummaryProvider provider = new OpenAiTicketSummaryProvider(new ObjectMapper(), structuredOutputAiClient);

    @Test
    void generateSummaryShouldBuildStructuredOutputRequestAndParseSummary() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                "{\"summary\":\"hello world\"}",
                "gpt-4.1-mini",
                120,
                44,
                164,
                null
        ));

        TicketSummaryProviderResult result = provider.generateSummary(sampleRequest());

        assertThat(result.summary()).isEqualTo("hello world");
        assertThat(result.modelId()).isEqualTo("gpt-4.1-mini");
        assertThat(result.totalTokens()).isEqualTo(164);
        assertThat(structuredOutputAiClient.lastRequest().schemaName()).isEqualTo("ticket_summary_response");
        assertThat(structuredOutputAiClient.lastRequest().maxOutputTokens()).isEqualTo(220);
        assertThat(structuredOutputAiClient.lastRequest().exampleJson()).contains("\"summary\"");
    }

    @Test
    void generateSummaryShouldRejectInvalidJsonPayload() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                "not-json",
                "gpt-4.1-mini",
                120,
                44,
                164,
                null
        ));

        assertThatThrownBy(() -> provider.generateSummary(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("provider summary payload is invalid");
    }

    @Test
    void generateSummaryShouldRejectMissingSummary() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                "{}",
                "gpt-4.1-mini",
                120,
                44,
                164,
                null
        ));

        assertThatThrownBy(() -> provider.generateSummary(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("provider summary payload is blank");
    }

    private TicketSummaryProviderRequest sampleRequest() {
        return new TicketSummaryProviderRequest(
                "ticket-ai-summary-provider-test-1",
                302L,
                "gpt-4.1-mini",
                1000,
                new TicketSummaryPrompt("ticket-summary-v1", "system", "user")
        );
    }
}
