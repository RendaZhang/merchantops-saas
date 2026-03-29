package com.renda.merchantops.infra.importjob;

import com.renda.merchantops.domain.importjob.ImportJobAiInteractionPageCriteria;
import com.renda.merchantops.infra.persistence.entity.AiInteractionRecordEntity;
import com.renda.merchantops.infra.repository.AiInteractionRecordRepository;
import com.renda.merchantops.infra.repository.ImportJobItemErrorRepository;
import com.renda.merchantops.infra.repository.ImportJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaImportJobAdapterTest {

    @Mock
    private ImportJobRepository importJobRepository;

    @Mock
    private ImportJobItemErrorRepository importJobItemErrorRepository;

    @Mock
    private AiInteractionRecordRepository aiInteractionRecordRepository;

    @Test
    void pageJobAiInteractionsShouldMapUsageFieldsAndUseStableSort() {
        JpaImportJobAdapter adapter = new JpaImportJobAdapter(
                importJobRepository,
                importJobItemErrorRepository,
                aiInteractionRecordRepository
        );
        AiInteractionRecordEntity record = new AiInteractionRecordEntity();
        record.setId(9103L);
        record.setInteractionType("FIX_RECOMMENDATION");
        record.setStatus("INVALID_RESPONSE");
        record.setOutputSummary(null);
        record.setPromptVersion("import-fix-recommendation-v1");
        record.setModelId("gpt-4.1-mini");
        record.setLatencyMs(251L);
        record.setRequestId("import-ai-fix-recommendation-invalid-response-1");
        record.setUsagePromptTokens(null);
        record.setUsageCompletionTokens(null);
        record.setUsageTotalTokens(null);
        record.setUsageCostMicros(null);
        record.setCreatedAt(LocalDateTime.of(2026, 3, 28, 10, 45));

        when(aiInteractionRecordRepository.searchPageByTenantIdAndEntity(
                eq(1L),
                eq("IMPORT_JOB"),
                eq(7001L),
                eq("FIX_RECOMMENDATION"),
                eq("INVALID_RESPONSE"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(record), PageRequest.of(0, 10), 1));

        var result = adapter.pageJobAiInteractions(
                1L,
                7001L,
                new ImportJobAiInteractionPageCriteria(0, 10, "FIX_RECOMMENDATION", "INVALID_RESPONSE")
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(aiInteractionRecordRepository).searchPageByTenantIdAndEntity(
                eq(1L),
                eq("IMPORT_JOB"),
                eq(7001L),
                eq("FIX_RECOMMENDATION"),
                eq("INVALID_RESPONSE"),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageableCaptor.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(9103L);
            assertThat(item.interactionType()).isEqualTo("FIX_RECOMMENDATION");
            assertThat(item.status()).isEqualTo("INVALID_RESPONSE");
            assertThat(item.requestId()).isEqualTo("import-ai-fix-recommendation-invalid-response-1");
            assertThat(item.usagePromptTokens()).isNull();
            assertThat(item.usageCompletionTokens()).isNull();
            assertThat(item.usageTotalTokens()).isNull();
            assertThat(item.usageCostMicros()).isNull();
            assertThat(item.createdAt()).isEqualTo(LocalDateTime.of(2026, 3, 28, 10, 45));
        });
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void findJobAiInteractionShouldMapScopedRecordWhenPresent() {
        JpaImportJobAdapter adapter = new JpaImportJobAdapter(
                importJobRepository,
                importJobItemErrorRepository,
                aiInteractionRecordRepository
        );
        AiInteractionRecordEntity record = new AiInteractionRecordEntity();
        record.setId(9103L);
        record.setInteractionType("FIX_RECOMMENDATION");
        record.setStatus("SUCCEEDED");
        record.setOutputSummary("Use selective replay for role-related rows only.");
        record.setPromptVersion("import-fix-recommendation-v1");
        record.setModelId("gpt-4.1-mini");
        record.setLatencyMs(566L);
        record.setRequestId("import-ai-fix-recommendation-req-1");
        record.setUsagePromptTokens(140);
        record.setUsageCompletionTokens(72);
        record.setUsageTotalTokens(212);
        record.setUsageCostMicros(null);
        record.setCreatedAt(LocalDateTime.of(2026, 3, 28, 10, 45));

        when(aiInteractionRecordRepository.findByIdAndTenantIdAndEntityTypeAndEntityId(9103L, 1L, "IMPORT_JOB", 7001L))
                .thenReturn(java.util.Optional.of(record));

        var result = adapter.findJobAiInteraction(1L, 7001L, 9103L);

        assertThat(result).isPresent();
        assertThat(result.get().interactionType()).isEqualTo("FIX_RECOMMENDATION");
        assertThat(result.get().status()).isEqualTo("SUCCEEDED");
        assertThat(result.get().requestId()).isEqualTo("import-ai-fix-recommendation-req-1");
    }
}
