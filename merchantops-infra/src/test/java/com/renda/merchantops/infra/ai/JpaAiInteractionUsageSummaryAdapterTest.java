package com.renda.merchantops.infra.ai;

import com.renda.merchantops.domain.ai.AiInteractionUsageSummaryCriteria;
import com.renda.merchantops.infra.repository.AiInteractionRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaAiInteractionUsageSummaryAdapterTest {

    @Mock
    private AiInteractionRecordRepository aiInteractionRecordRepository;

    @Mock
    private AiInteractionRecordRepository.AiInteractionUsageSummaryTotalsView totalsView;

    @Mock
    private AiInteractionRecordRepository.AiInteractionUsageByInteractionTypeView byInteractionTypeView;

    @Mock
    private AiInteractionRecordRepository.AiInteractionUsageByStatusView byStatusView;

    @Test
    void summarizeShouldMapTotalsAndStableBreakdowns() {
        JpaAiInteractionUsageSummaryAdapter adapter = new JpaAiInteractionUsageSummaryAdapter(aiInteractionRecordRepository);
        AiInteractionUsageSummaryCriteria criteria = new AiInteractionUsageSummaryCriteria(
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 5, 23, 59),
                "TICKET",
                "SUMMARY",
                "SUCCEEDED"
        );
        when(totalsView.getTotalInteractions()).thenReturn(3L);
        when(totalsView.getSucceededCount()).thenReturn(2L);
        when(totalsView.getFailedCount()).thenReturn(1L);
        when(totalsView.getTotalPromptTokens()).thenReturn(260L);
        when(totalsView.getTotalCompletionTokens()).thenReturn(100L);
        when(totalsView.getTotalTokens()).thenReturn(360L);
        when(totalsView.getTotalCostMicros()).thenReturn(4000L);
        when(byInteractionTypeView.getInteractionType()).thenReturn("SUMMARY");
        when(byInteractionTypeView.getInteractionCount()).thenReturn(3L);
        when(byInteractionTypeView.getSucceededCount()).thenReturn(2L);
        when(byInteractionTypeView.getFailedCount()).thenReturn(1L);
        when(byInteractionTypeView.getTotalTokens()).thenReturn(360L);
        when(byInteractionTypeView.getTotalCostMicros()).thenReturn(4000L);
        when(byStatusView.getStatus()).thenReturn("SUCCEEDED");
        when(byStatusView.getInteractionCount()).thenReturn(2L);
        when(byStatusView.getTotalTokens()).thenReturn(300L);
        when(byStatusView.getTotalCostMicros()).thenReturn(3900L);
        when(aiInteractionRecordRepository.summarizeUsageByTenant(
                eq(1L), eq(criteria.from()), eq(criteria.to()), eq("TICKET"), eq("SUMMARY"), eq("SUCCEEDED")
        )).thenReturn(totalsView);
        when(aiInteractionRecordRepository.summarizeUsageByInteractionType(
                eq(1L), eq(criteria.from()), eq(criteria.to()), eq("TICKET"), eq("SUMMARY"), eq("SUCCEEDED")
        )).thenReturn(List.of(byInteractionTypeView));
        when(aiInteractionRecordRepository.summarizeUsageByStatus(
                eq(1L), eq(criteria.from()), eq(criteria.to()), eq("TICKET"), eq("SUMMARY"), eq("SUCCEEDED")
        )).thenReturn(List.of(byStatusView));

        var result = adapter.summarize(1L, criteria);

        assertThat(result.totalInteractions()).isEqualTo(3L);
        assertThat(result.succeededCount()).isEqualTo(2L);
        assertThat(result.failedCount()).isEqualTo(1L);
        assertThat(result.totalTokens()).isEqualTo(360L);
        assertThat(result.byInteractionType()).singleElement().satisfies(item -> {
            assertThat(item.interactionType()).isEqualTo("SUMMARY");
            assertThat(item.count()).isEqualTo(3L);
            assertThat(item.succeededCount()).isEqualTo(2L);
            assertThat(item.failedCount()).isEqualTo(1L);
            assertThat(item.totalTokens()).isEqualTo(360L);
            assertThat(item.totalCostMicros()).isEqualTo(4000L);
        });
        assertThat(result.byStatus()).singleElement().satisfies(item -> {
            assertThat(item.status()).isEqualTo("SUCCEEDED");
            assertThat(item.count()).isEqualTo(2L);
            assertThat(item.totalTokens()).isEqualTo(300L);
            assertThat(item.totalCostMicros()).isEqualTo(3900L);
        });

        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(aiInteractionRecordRepository).summarizeUsageByTenant(
                eq(1L),
                fromCaptor.capture(),
                eq(criteria.to()),
                eq("TICKET"),
                eq("SUMMARY"),
                eq("SUCCEEDED")
        );
        assertThat(fromCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 4, 1, 0, 0));
    }
}
