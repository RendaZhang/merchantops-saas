package com.renda.merchantops.api.ai.ticket.replydraft;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.ai.client.StructuredOutputAiResponse;
import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.support.StubStructuredOutputAiClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiTicketReplyDraftProviderTest {

    private final StubStructuredOutputAiClient structuredOutputAiClient = new StubStructuredOutputAiClient();
    private final OpenAiTicketReplyDraftProvider provider = new OpenAiTicketReplyDraftProvider(new ObjectMapper(), structuredOutputAiClient);

    @Test
    void generateReplyDraftShouldBuildStructuredOutputRequestAndParsePayload() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                "{\"opening\":\"Quick update.\",\"body\":\"Body.\",\"nextStep\":\"Next.\",\"closing\":\"Closing.\"}",
                "gpt-4.1-mini",
                110,
                70,
                180,
                null
        ));

        TicketReplyDraftProviderResult result = provider.generateReplyDraft(sampleRequest());

        assertThat(result.opening()).isEqualTo("Quick update.");
        assertThat(result.body()).isEqualTo("Body.");
        assertThat(result.nextStep()).isEqualTo("Next.");
        assertThat(result.closing()).isEqualTo("Closing.");
        assertThat(structuredOutputAiClient.lastRequest().schemaName()).isEqualTo("ticket_reply_draft_response");
        assertThat(structuredOutputAiClient.lastRequest().exampleJson()).contains("\"nextStep\"");
    }

    @Test
    void generateReplyDraftShouldRejectInvalidJsonPayload() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                "not-json",
                "gpt-4.1-mini",
                110,
                70,
                180,
                null
        ));

        assertThatThrownBy(() -> provider.generateReplyDraft(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("provider reply draft payload is invalid");
    }

    @Test
    void generateReplyDraftShouldRejectMissingNextStep() {
        structuredOutputAiClient.willReturn(new StructuredOutputAiResponse(
                "{\"opening\":\"Quick update.\",\"body\":\"Body.\",\"closing\":\"Closing.\"}",
                "gpt-4.1-mini",
                110,
                70,
                180,
                null
        ));

        assertThatThrownBy(() -> provider.generateReplyDraft(sampleRequest()))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("provider reply draft payload is missing nextStep");
    }

    private TicketReplyDraftProviderRequest sampleRequest() {
        return new TicketReplyDraftProviderRequest(
                "ticket-ai-reply-draft-provider-test-1",
                302L,
                "gpt-4.1-mini",
                1000,
                new TicketReplyDraftPrompt("ticket-reply-draft-v1", "system", "user")
        );
    }
}
