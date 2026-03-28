package com.renda.merchantops.api.importjob;

import com.renda.merchantops.api.dto.importjob.query.ImportJobAiInteractionPageQuery;
import com.renda.merchantops.api.dto.importjob.query.ImportJobAiInteractionPageResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobErrorPageQuery;
import com.renda.merchantops.api.dto.importjob.query.ImportJobErrorPageResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobPageQuery;
import com.renda.merchantops.api.dto.importjob.query.ImportJobPageResponse;
import com.renda.merchantops.domain.importjob.ImportJobAiInteractionItem;
import com.renda.merchantops.domain.importjob.ImportJobAiInteractionPageCriteria;
import com.renda.merchantops.domain.importjob.ImportJobAiInteractionPageResult;
import com.renda.merchantops.domain.importjob.ImportJobDetail;
import com.renda.merchantops.domain.importjob.ImportJobErrorCount;
import com.renda.merchantops.domain.importjob.ImportJobErrorPageCriteria;
import com.renda.merchantops.domain.importjob.ImportJobErrorPageResult;
import com.renda.merchantops.domain.importjob.ImportJobErrorRecord;
import com.renda.merchantops.domain.importjob.ImportJobPageCriteria;
import com.renda.merchantops.domain.importjob.ImportJobPageResult;
import com.renda.merchantops.domain.importjob.ImportJobQueryUseCase;
import com.renda.merchantops.domain.importjob.ImportJobRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportJobQueryServiceTest {

    @Mock
    private ImportJobQueryUseCase importJobQueryUseCase;

    private ImportJobQueryService importJobQueryService;

    @BeforeEach
    void setUp() {
        importJobQueryService = new ImportJobQueryService(importJobQueryUseCase, new ImportJobResponseMapper());
    }

    @Test
    void pageJobsShouldNormalizeQueryFiltersMapListFieldsAndUseStableSort() {
        ImportJobRecord record = new ImportJobRecord(
                1L, 1L, "USER_CSV", "CSV", "users.csv", "1/key.csv", null,
                "FAILED", 101L, "req-1", 3, 1, 2, "completed with some row errors",
                LocalDateTime.now(), null, null
        );
        when(importJobQueryUseCase.pageJobs(eq(1L), eq(new ImportJobPageCriteria(-1, 500, " FAILED ", " USER_CSV ", 101L, true))))
                .thenReturn(new ImportJobPageResult(List.of(record), 0, 100, 1, 1));

        ImportJobPageQuery query = new ImportJobPageQuery(-1, 500, " FAILED ", " USER_CSV ", 101L, true);
        ImportJobPageResponse response = importJobQueryService.pageJobs(1L, query);

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.requestedBy()).isEqualTo(101L);
            assertThat(item.hasFailures()).isTrue();
            assertThat(item.errorSummary()).isEqualTo("completed with some row errors");
        });
    }

    @Test
    void pageJobErrorsShouldNormalizeFilterAndMapItems() {
        ImportJobErrorRecord error = new ImportJobErrorRecord(
                901L, 1L, 1L, 5, "UNKNOWN_ROLE", "role missing", "row-5", LocalDateTime.now()
        );
        when(importJobQueryUseCase.pageJobErrors(eq(1L), eq(1L), eq(new ImportJobErrorPageCriteria(-1, 500, " UNKNOWN_ROLE "))))
                .thenReturn(new ImportJobErrorPageResult(List.of(error), 0, 100, 1, 1));

        ImportJobErrorPageResponse response = importJobQueryService.pageJobErrors(1L, 1L,
                new ImportJobErrorPageQuery(-1, 500, " UNKNOWN_ROLE "));

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.rowNumber()).isEqualTo(5);
            assertThat(item.errorCode()).isEqualTo("UNKNOWN_ROLE");
            assertThat(item.rawPayload()).isEqualTo("row-5");
        });
    }

    @Test
    void pageJobAiInteractionsShouldMapDomainPageAndForwardCriteria() {
        ImportJobAiInteractionItem item = new ImportJobAiInteractionItem(
                9101L,
                "ERROR_SUMMARY",
                "SUCCEEDED",
                "The job is dominated by tenant role validation failures.",
                "import-error-summary-v1",
                "gpt-4.1-mini",
                512L,
                "import-ai-error-summary-req-1",
                140,
                72,
                212,
                null,
                LocalDateTime.of(2026, 3, 28, 10, 40)
        );
        when(importJobQueryUseCase.pageJobAiInteractions(
                eq(1L),
                eq(1L),
                eq(new ImportJobAiInteractionPageCriteria(-1, 500, " ERROR_SUMMARY ", " SUCCEEDED "))
        )).thenReturn(new ImportJobAiInteractionPageResult(List.of(item), 0, 100, 1, 1));

        ImportJobAiInteractionPageResponse response = importJobQueryService.pageJobAiInteractions(
                1L,
                1L,
                new ImportJobAiInteractionPageQuery(-1, 500, " ERROR_SUMMARY ", " SUCCEEDED ")
        );

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.items()).singleElement().satisfies(mapped -> {
            assertThat(mapped.id()).isEqualTo(9101L);
            assertThat(mapped.interactionType()).isEqualTo("ERROR_SUMMARY");
            assertThat(mapped.requestId()).isEqualTo("import-ai-error-summary-req-1");
            assertThat(mapped.usageTotalTokens()).isEqualTo(212);
            assertThat(mapped.usageCostMicros()).isNull();
        });
    }

    @Test
    void getJobDetailShouldReturnItemErrorsAndErrorCodeCounts() {
        ImportJobRecord job = new ImportJobRecord(
                1L, 1L, "USER_CSV", "CSV", "users.csv", "1/key.csv", 77L,
                "FAILED", 101L, "req-1", 1, 0, 1, null, LocalDateTime.now(), null, null
        );
        ImportJobErrorRecord error = new ImportJobErrorRecord(
                900L, 1L, 1L, 2, "INVALID_ROW_SHAPE", "column count mismatch", null, LocalDateTime.now()
        );
        when(importJobQueryUseCase.getJobDetail(1L, 1L)).thenReturn(
                new ImportJobDetail(job, List.of(new ImportJobErrorCount("INVALID_ROW_SHAPE", 1L)), List.of(error))
        );

        var response = importJobQueryService.getJobDetail(1L, 1L);
        assertThat(response.sourceJobId()).isEqualTo(77L);
        assertThat(response.itemErrors()).hasSize(1);
        assertThat(response.errorCodeCounts()).singleElement().satisfies(item -> {
            assertThat(item.errorCode()).isEqualTo("INVALID_ROW_SHAPE");
            assertThat(item.count()).isEqualTo(1L);
        });
    }
}
