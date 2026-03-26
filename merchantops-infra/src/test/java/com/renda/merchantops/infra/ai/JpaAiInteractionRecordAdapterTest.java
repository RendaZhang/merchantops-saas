package com.renda.merchantops.infra.ai;

import com.renda.merchantops.domain.ai.NewAiInteractionRecord;
import com.renda.merchantops.infra.persistence.entity.AiInteractionRecordEntity;
import com.renda.merchantops.infra.repository.AiInteractionRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JpaAiInteractionRecordAdapterTest {

    @Mock
    private AiInteractionRecordRepository aiInteractionRecordRepository;

    @Test
    void saveShouldPersistNormalizedDomainInteractionRecordFields() {
        JpaAiInteractionRecordAdapter adapter = new JpaAiInteractionRecordAdapter(aiInteractionRecordRepository);

        adapter.save(new NewAiInteractionRecord(
                1L,
                101L,
                "req-1",
                "TICKET",
                9L,
                "SUMMARY",
                "ticket-summary-v1",
                "gpt-4.1-mini",
                "SUCCEEDED",
                55L,
                "Issue: summary",
                12,
                6,
                18,
                400L,
                LocalDateTime.of(2026, 3, 26, 10, 0)
        ));

        ArgumentCaptor<AiInteractionRecordEntity> entityCaptor = ArgumentCaptor.forClass(AiInteractionRecordEntity.class);
        verify(aiInteractionRecordRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getEntityType()).isEqualTo("TICKET");
        assertThat(entityCaptor.getValue().getInteractionType()).isEqualTo("SUMMARY");
        assertThat(entityCaptor.getValue().getStatus()).isEqualTo("SUCCEEDED");
        assertThat(entityCaptor.getValue().getUsageTotalTokens()).isEqualTo(18);
    }
}
