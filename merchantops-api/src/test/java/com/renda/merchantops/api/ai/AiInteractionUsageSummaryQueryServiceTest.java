package com.renda.merchantops.api.ai;

import com.renda.merchantops.api.dto.ai.query.AiInteractionUsageSummaryQuery;
import com.renda.merchantops.api.dto.ai.query.AiInteractionUsageSummaryResponse;
import com.renda.merchantops.domain.ai.AiInteractionUsageByInteractionType;
import com.renda.merchantops.domain.ai.AiInteractionUsageByStatus;
import com.renda.merchantops.domain.ai.AiInteractionUsageSummary;
import com.renda.merchantops.domain.ai.AiInteractionUsageSummaryCriteria;
import com.renda.merchantops.domain.ai.AiInteractionUsageSummaryUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiInteractionUsageSummaryQueryServiceTest {

    @Mock
    private AiInteractionUsageSummaryUseCase aiInteractionUsageSummaryUseCase;

    @InjectMocks
    private AiInteractionUsageSummaryQueryService aiInteractionUsageSummaryQueryService;

    @Test
    void getUsageSummaryShouldMapDomainResultAndForwardCriteria() {
        AiInteractionUsageSummaryQuery query = new AiInteractionUsageSummaryQuery(
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 5, 23, 59, 59),
                " TICKET ",
                " SUMMARY ",
                " SUCCEEDED "
        );
        when(aiInteractionUsageSummaryUseCase.summarize(eq(1L), any(AiInteractionUsageSummaryCriteria.class)))
                .thenReturn(new AiInteractionUsageSummary(
                        query.getFrom(),
                        query.getTo(),
                        3L,
                        2L,
                        1L,
                        260L,
                        100L,
                        360L,
                        4000L,
                        List.of(new AiInteractionUsageByInteractionType("SUMMARY", 3L, 2L, 1L, 360L, 4000L)),
                        List.of(new AiInteractionUsageByStatus("SUCCEEDED", 2L, 300L, 3900L))
                ));

        AiInteractionUsageSummaryResponse response = aiInteractionUsageSummaryQueryService.getUsageSummary(1L, query);

        ArgumentCaptor<AiInteractionUsageSummaryCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(AiInteractionUsageSummaryCriteria.class);
        verify(aiInteractionUsageSummaryUseCase).summarize(eq(1L), criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue()).isEqualTo(new AiInteractionUsageSummaryCriteria(
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 5, 23, 59, 59),
                " TICKET ",
                " SUMMARY ",
                " SUCCEEDED "
        ));
        assertThat(response.totalInteractions()).isEqualTo(3L);
        assertThat(response.byInteractionType()).singleElement().satisfies(item -> {
            assertThat(item.interactionType()).isEqualTo("SUMMARY");
            assertThat(item.count()).isEqualTo(3L);
        });
        assertThat(response.byStatus()).singleElement().satisfies(item -> {
            assertThat(item.status()).isEqualTo("SUCCEEDED");
            assertThat(item.count()).isEqualTo(2L);
        });
    }
}
