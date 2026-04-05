package com.renda.merchantops.domain.ai;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiInteractionUsageSummaryDomainServiceTest {

    @Test
    void summarizeShouldNormalizeFiltersByTrimmingOnly() {
        CapturingAiInteractionUsageSummaryQueryPort port = new CapturingAiInteractionUsageSummaryQueryPort();
        AiInteractionUsageSummaryUseCase useCase = new AiInteractionUsageSummaryDomainService(port);

        useCase.summarize(1L, new AiInteractionUsageSummaryCriteria(
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 5, 23, 59),
                " TICKET ",
                " SUMMARY ",
                " SUCCEEDED "
        ));

        assertThat(port.criteria).isEqualTo(new AiInteractionUsageSummaryCriteria(
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 5, 23, 59),
                "TICKET",
                "SUMMARY",
                "SUCCEEDED"
        ));
    }

    @Test
    void summarizeShouldRejectUnsupportedEntityType() {
        AiInteractionUsageSummaryUseCase useCase = new AiInteractionUsageSummaryDomainService(
                new CapturingAiInteractionUsageSummaryQueryPort()
        );

        assertThatThrownBy(() -> useCase.summarize(1L, new AiInteractionUsageSummaryCriteria(
                null,
                null,
                "ticket",
                null,
                null
        )))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST))
                .hasMessage("entityType must be one of TICKET, IMPORT_JOB");
    }

    @Test
    void summarizeShouldRejectWhenFromIsAfterTo() {
        AiInteractionUsageSummaryUseCase useCase = new AiInteractionUsageSummaryDomainService(
                new CapturingAiInteractionUsageSummaryQueryPort()
        );

        assertThatThrownBy(() -> useCase.summarize(1L, new AiInteractionUsageSummaryCriteria(
                LocalDateTime.of(2026, 4, 6, 0, 0),
                LocalDateTime.of(2026, 4, 5, 0, 0),
                null,
                null,
                null
        )))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST))
                .hasMessage("from must be before or equal to to");
    }

    private static final class CapturingAiInteractionUsageSummaryQueryPort implements AiInteractionUsageSummaryQueryPort {

        private AiInteractionUsageSummaryCriteria criteria;

        @Override
        public AiInteractionUsageSummary summarize(Long tenantId, AiInteractionUsageSummaryCriteria criteria) {
            this.criteria = criteria;
            return new AiInteractionUsageSummary(
                    criteria.from(),
                    criteria.to(),
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    List.of(),
                    List.of()
            );
        }
    }
}
