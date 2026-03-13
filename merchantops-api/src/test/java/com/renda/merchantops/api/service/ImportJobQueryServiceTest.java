package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.importjob.query.ImportJobErrorPageQuery;
import com.renda.merchantops.api.dto.importjob.query.ImportJobErrorPageResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobPageQuery;
import com.renda.merchantops.api.dto.importjob.query.ImportJobPageResponse;
import com.renda.merchantops.infra.persistence.entity.ImportJobEntity;
import com.renda.merchantops.infra.persistence.entity.ImportJobItemErrorEntity;
import com.renda.merchantops.infra.repository.ImportJobItemErrorRepository;
import com.renda.merchantops.infra.repository.ImportJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportJobQueryServiceTest {

    @Mock
    private ImportJobRepository importJobRepository;
    @Mock
    private ImportJobItemErrorRepository importJobItemErrorRepository;

    @InjectMocks
    private ImportJobQueryService importJobQueryService;

    @Test
    void pageJobsShouldNormalizeQueryFiltersMapListFieldsAndUseStableSort() {
        ImportJobEntity entity = new ImportJobEntity();
        entity.setId(1L);
        entity.setTenantId(1L);
        entity.setImportType("USER_CSV");
        entity.setSourceType("CSV");
        entity.setSourceFilename("users.csv");
        entity.setStatus("FAILED");
        entity.setRequestedBy(101L);
        entity.setTotalCount(3);
        entity.setSuccessCount(1);
        entity.setFailureCount(2);
        entity.setErrorSummary("completed with some row errors");
        entity.setCreatedAt(LocalDateTime.now());
        when(importJobRepository.searchPageByTenantId(eq(1L), eq("FAILED"), eq("USER_CSV"), eq(101L), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(entity)));

        ImportJobPageQuery query = new ImportJobPageQuery(-1, 500, " FAILED ", " USER_CSV ", 101L, true);
        ImportJobPageResponse response = importJobQueryService.pageJobs(1L, query);

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.requestedBy()).isEqualTo(101L);
            assertThat(item.hasFailures()).isTrue();
            assertThat(item.errorSummary()).isEqualTo("completed with some row errors");
        });
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(importJobRepository).searchPageByTenantId(eq(1L), eq("FAILED"), eq("USER_CSV"), eq(101L), eq(true), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(100);
        assertThat(pageable.getSort().getOrderFor("createdAt")).extracting(Sort.Order::getDirection).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getSort().getOrderFor("id")).extracting(Sort.Order::getDirection).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void pageJobErrorsShouldNormalizeFilterAndMapItems() {
        ImportJobEntity entity = new ImportJobEntity();
        entity.setId(1L);
        entity.setTenantId(1L);

        ImportJobItemErrorEntity error = new ImportJobItemErrorEntity();
        error.setId(901L);
        error.setRowNumber(5);
        error.setErrorCode("UNKNOWN_ROLE");
        error.setErrorMessage("role missing");
        error.setRawPayload("row-5");
        error.setCreatedAt(LocalDateTime.now());

        when(importJobRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(entity));
        when(importJobItemErrorRepository.searchPageByTenantIdAndImportJobId(eq(1L), eq(1L), eq("UNKNOWN_ROLE"), any()))
                .thenReturn(new PageImpl<>(List.of(error)));

        ImportJobErrorPageResponse response = importJobQueryService.pageJobErrors(1L, 1L,
                new ImportJobErrorPageQuery(-1, 500, " UNKNOWN_ROLE "));

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.rowNumber()).isEqualTo(5);
            assertThat(item.errorCode()).isEqualTo("UNKNOWN_ROLE");
            assertThat(item.rawPayload()).isEqualTo("row-5");
        });
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(importJobItemErrorRepository).searchPageByTenantIdAndImportJobId(eq(1L), eq(1L), eq("UNKNOWN_ROLE"), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(100);
    }

    @Test
    void getJobDetailShouldReturnItemErrorsAndErrorCodeCounts() {
        ImportJobEntity entity = new ImportJobEntity();
        entity.setId(1L);
        entity.setTenantId(1L);
        entity.setImportType("USER_CSV");
        entity.setSourceType("CSV");
        entity.setSourceFilename("users.csv");
        entity.setStorageKey("1/key.csv");
        entity.setSourceJobId(77L);
        entity.setStatus("FAILED");
        entity.setRequestedBy(101L);
        entity.setRequestId("req-1");
        entity.setTotalCount(1);
        entity.setSuccessCount(0);
        entity.setFailureCount(1);
        entity.setCreatedAt(LocalDateTime.now());

        ImportJobItemErrorEntity error = new ImportJobItemErrorEntity();
        error.setId(900L);
        error.setRowNumber(2);
        error.setErrorCode("INVALID_ROW_SHAPE");
        error.setErrorMessage("column count mismatch");
        error.setCreatedAt(LocalDateTime.now());

        when(importJobRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(entity));
        when(importJobItemErrorRepository.summarizeErrorCodesByTenantIdAndImportJobId(1L, 1L)).thenReturn(List.of(
                new ImportJobItemErrorRepository.ImportJobErrorCodeCountView() {
                    @Override
                    public String getErrorCode() {
                        return "INVALID_ROW_SHAPE";
                    }

                    @Override
                    public long getErrorCount() {
                        return 1L;
                    }
                }
        ));
        when(importJobItemErrorRepository.findAllByTenantIdAndImportJobIdOrderByRowNumberAscIdAsc(1L, 1L)).thenReturn(List.of(error));

        var response = importJobQueryService.getJobDetail(1L, 1L);
        assertThat(response.sourceJobId()).isEqualTo(77L);
        assertThat(response.itemErrors()).hasSize(1);
        assertThat(response.errorCodeCounts()).singleElement().satisfies(item -> {
            assertThat(item.errorCode()).isEqualTo("INVALID_ROW_SHAPE");
            assertThat(item.count()).isEqualTo(1L);
        });
    }
}
