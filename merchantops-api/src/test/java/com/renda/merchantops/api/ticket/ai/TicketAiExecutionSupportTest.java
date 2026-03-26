package com.renda.merchantops.api.ticket.ai;

import com.renda.merchantops.api.ai.client.StructuredOutputAiResponse;
import com.renda.merchantops.api.ai.core.AiProviderException;
import com.renda.merchantops.api.ai.core.AiProviderFailureType;
import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.domain.ai.AiInteractionRecordCommand;
import com.renda.merchantops.domain.ai.AiInteractionRecordUseCase;
import com.renda.merchantops.domain.ai.AiInteractionStatus;
import com.renda.merchantops.domain.shared.error.BizException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TicketAiExecutionSupportTest {

    private final AiInteractionRecordUseCase recordUseCase = mock(AiInteractionRecordUseCase.class);
    private final TicketAiExecutionSupport support = new TicketAiExecutionSupport(recordUseCase);

    @Test
    void assertAvailableShouldRecordFeatureDisabledBeforeThrowing() {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setEnabled(false);

        assertThatThrownBy(() -> support.assertAvailable(
                11L, 22L, "req-1", 33L, "SUMMARY", "ticket-summary-v1", "gpt-4.1-mini",
                aiProperties, "disabled", "unavailable"
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("disabled");

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.FEATURE_DISABLED);
        assertThat(commandCaptor.getValue().interactionType()).isEqualTo("SUMMARY");
    }

    @Test
    void assertAvailableShouldRecordProviderNotConfiguredBeforeThrowing() {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setEnabled(true);

        assertThatThrownBy(() -> support.assertAvailable(
                11L, 22L, "req-2", 33L, "TRIAGE", "ticket-triage-v1", "gpt-4.1-mini",
                aiProperties, "disabled", "unavailable"
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("unavailable");

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.PROVIDER_NOT_CONFIGURED);
        assertThat(commandCaptor.getValue().interactionType()).isEqualTo("TRIAGE");
    }

    @Test
    void recordSuccessShouldAssembleUsageFieldsIntoInteractionCommand() {
        support.recordSuccess(
                11L,
                22L,
                "req-3",
                33L,
                "SUMMARY",
                "ticket-summary-v1",
                "gpt-4.1-mini",
                45L,
                "Issue: summary",
                new StructuredOutputAiResponse("{}", "gpt-4.1-mini", 120, 44, 164, 900L)
        );

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().entityType()).isEqualTo("TICKET");
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.SUCCEEDED);
        assertThat(commandCaptor.getValue().usagePromptTokens()).isEqualTo(120);
        assertThat(commandCaptor.getValue().usageCompletionTokens()).isEqualTo(44);
        assertThat(commandCaptor.getValue().usageTotalTokens()).isEqualTo(164);
        assertThat(commandCaptor.getValue().usageCostMicros()).isEqualTo(900L);
    }

    @Test
    void recordFailureShouldMapProviderFailureTypeIntoInteractionStatus() {
        support.recordFailure(
                11L,
                22L,
                "req-4",
                33L,
                "REPLY_DRAFT",
                "ticket-reply-draft-v1",
                "gpt-4.1-mini",
                AiProviderFailureType.INVALID_RESPONSE,
                19L
        );

        ArgumentCaptor<AiInteractionRecordCommand> commandCaptor = ArgumentCaptor.forClass(AiInteractionRecordCommand.class);
        verify(recordUseCase).record(commandCaptor.capture());
        assertThat(commandCaptor.getValue().status()).isEqualTo(AiInteractionStatus.INVALID_RESPONSE);
        assertThat(commandCaptor.getValue().outputSummary()).isNull();
    }

    @Test
    void toBizExceptionShouldUseTimeoutMessageOnlyForTimeoutFailures() {
        BizException timeout = support.toBizException(
                new AiProviderException(AiProviderFailureType.TIMEOUT, "timeout"),
                "timed out",
                "unavailable"
        );
        BizException unavailable = support.toBizException(
                new AiProviderException(AiProviderFailureType.UNAVAILABLE, "unavailable"),
                "timed out",
                "unavailable"
        );

        assertThat(timeout.getMessage()).isEqualTo("timed out");
        assertThat(unavailable.getMessage()).isEqualTo("unavailable");
    }
}
