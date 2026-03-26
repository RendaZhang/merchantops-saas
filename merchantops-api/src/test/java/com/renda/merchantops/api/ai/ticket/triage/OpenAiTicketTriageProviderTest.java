package com.renda.merchantops.api.ai.ticket.triage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.ai.client.StructuredOutputAiResponse;
import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.support.StubStructuredOutputAiClient;
import com.renda.merchantops.api.dto.ticket.query.TicketAiTriagePriority;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiTicketTriageProviderTest {

    private final StubStructuredOutputAiClient structuredOutputAiClient = new StubStructuredOutputAiClient();
    private final OpenAiTicketTriageProvider provider = new OpenAiTicketTriageProvider(new ObjectMapper(), structuredOutputAiClient);

    @Test
    void generateTriageShouldBuildStructuredOutputRequestAndParsePayload() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                "{\"classification\":\"DEVICE_ISSUE\",\"priority\":\"HIGH\",\"reasoning\":\"Operations are blocked.\"}",
                "gpt-4.1-mini",
                90,
                60,
                150,
                null
        ));

        TicketTriageProviderResult result = provider.generateTriage(sampleRequest());

        assertThat(result.classification()).isEqualTo("DEVICE_ISSUE");
        assertThat(result.priority()).isEqualTo(TicketAiTriagePriority.HIGH);
        assertThat(result.reasoning()).isEqualTo("Operations are blocked.");
        assertThat(structuredOutputAiClient.lastRequest().schemaName()).isEqualTo("ticket_triage_response");
        assertThat(structuredOutputAiClient.lastRequest().exampleJson()).contains("\"priority\"");
    }

    @Test
    void generateTriageShouldRejectMissingClassification() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                "{\"priority\":\"HIGH\",\"reasoning\":\"Operations are blocked.\"}",
                "gpt-4.1-mini",
                90,
                60,
                150,
                null
        ));

        assertThatThrownBy(() -> provider.generateTriage(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("provider triage payload is missing classification");
    }

    @Test
    void generateTriageShouldRejectInvalidPriority() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                "{\"classification\":\"DEVICE_ISSUE\",\"priority\":\"URGENT\",\"reasoning\":\"Operations are blocked.\"}",
                "gpt-4.1-mini",
                90,
                60,
                150,
                null
        ));

        assertThatThrownBy(() -> provider.generateTriage(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("provider triage payload has invalid priority");
    }

    private TicketTriageProviderRequest sampleRequest() {
        return new TicketTriageProviderRequest(
                "ticket-ai-triage-provider-test-1",
                302L,
                "gpt-4.1-mini",
                1000,
                new TicketTriagePrompt("ticket-triage-v1", "system", "user")
        );
    }
}
