package com.renda.merchantops.api.service;

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
    void pageJobsShouldNormalizeQueryAndUseStableSort() {
        ImportJobEntity entity = new ImportJobEntity();
        entity.setId(1L);
        entity.setTenantId(1L);
        entity.setImportType("USER_CSV");
        entity.setSourceType("CSV");
        entity.setSourceFilename("users.csv");
        entity.setStatus("QUEUED");
        entity.setTotalCount(0);
        entity.setSuccessCount(0);
        entity.setFailureCount(0);
        entity.setCreatedAt(LocalDateTime.now());
        when(importJobRepository.findAllByTenantId(eq(1L), any())).thenReturn(new PageImpl<>(List.of(entity)));

        ImportJobPageResponse response = importJobQueryService.pageJobs(1L, new ImportJobPageQuery());

        assertThat(response.total()).isEqualTo(1);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(importJobRepository).findAllByTenantId(eq(1L), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getSort().getOrderFor("createdAt")).extracting(Sort.Order::getDirection).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getSort().getOrderFor("id")).extracting(Sort.Order::getDirection).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getJobDetailShouldReturnItemErrors() {
        ImportJobEntity entity = new ImportJobEntity();
        entity.setId(1L);
        entity.setTenantId(1L);
        entity.setImportType("USER_CSV");
        entity.setSourceType("CSV");
        entity.setSourceFilename("users.csv");
        entity.setStorageKey("1/key.csv");
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
        when(importJobItemErrorRepository.findAllByTenantIdAndImportJobIdOrderByIdAsc(1L, 1L)).thenReturn(List.of(error));

        assertThat(importJobQueryService.getJobDetail(1L, 1L).itemErrors()).hasSize(1);
    }
}
