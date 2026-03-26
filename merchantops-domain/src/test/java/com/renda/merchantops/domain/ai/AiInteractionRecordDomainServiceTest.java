package com.renda.merchantops.domain.ai;

import com.renda.merchantops.domain.shared.error.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiInteractionRecordDomainServiceTest {

    @Test
    void recordShouldNormalizeKeysAndOptionalFields() {
        CapturingAiInteractionRecordPort port = new CapturingAiInteractionRecordPort();
        AiInteractionRecordUseCase useCase = new AiInteractionRecordDomainService(port);

        useCase.record(new AiInteractionRecordCommand(
                1L,
                101L,
                "req-1",
                " ticket ",
                9L,
                " summary ",
                " ticket-summary-v1 ",
                " gpt-4.1-mini ",
                AiInteractionStatus.SUCCEEDED,
                55L,
                " Issue: summary ",
                10,
                5,
                15,
                300L
        ));

        assertThat(port.savedRecord.entityType()).isEqualTo("TICKET");
        assertThat(port.savedRecord.interactionType()).isEqualTo("SUMMARY");
        assertThat(port.savedRecord.promptVersion()).isEqualTo("ticket-summary-v1");
        assertThat(port.savedRecord.modelId()).isEqualTo("gpt-4.1-mini");
        assertThat(port.savedRecord.outputSummary()).isEqualTo("Issue: summary");
        assertThat(port.savedRecord.status()).isEqualTo("SUCCEEDED");
        assertThat(port.savedRecord.createdAt()).isNotNull();
    }

    @Test
    void recordShouldRejectMissingPromptVersion() {
        AiInteractionRecordUseCase useCase = new AiInteractionRecordDomainService(new CapturingAiInteractionRecordPort());

        assertThatThrownBy(() -> useCase.record(new AiInteractionRecordCommand(
                1L,
                101L,
                "req-1",
                "TICKET",
                9L,
                "SUMMARY",
                " ",
                null,
                AiInteractionStatus.SUCCEEDED,
                55L,
                null,
                null,
                null,
                null,
                null
        )))
                .isInstanceOf(BizException.class)
                .hasMessage("ai interaction required fields missing");
    }

    private static final class CapturingAiInteractionRecordPort implements AiInteractionRecordPort {

        private NewAiInteractionRecord savedRecord;

        @Override
        public void save(NewAiInteractionRecord record) {
            this.savedRecord = record;
        }
    }
}
