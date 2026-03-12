package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.importjob.query.ImportJobDetailResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobErrorItemResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobListItemResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobPageQuery;
import com.renda.merchantops.api.dto.importjob.query.ImportJobPageResponse;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.ImportJobEntity;
import com.renda.merchantops.infra.repository.ImportJobItemErrorRepository;
import com.renda.merchantops.infra.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ImportJobQueryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final ImportJobRepository importJobRepository;
    private final ImportJobItemErrorRepository importJobItemErrorRepository;

    public ImportJobPageResponse pageJobs(Long tenantId, ImportJobPageQuery query) {
        ImportJobPageQuery normalizedQuery = normalizeQuery(query);
        PageRequest pageable = PageRequest.of(normalizedQuery.getPage(), normalizedQuery.getSize(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<ImportJobEntity> page = importJobRepository.searchPageByTenantId(
                tenantId,
                normalizedQuery.getStatus(),
                normalizedQuery.getImportType(),
                normalizedQuery.getRequestedBy(),
                Boolean.TRUE.equals(normalizedQuery.getHasFailuresOnly()),
                pageable
        );
        return new ImportJobPageResponse(
                page.getContent().stream().map(this::toListItem).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    public ImportJobDetailResponse getJobDetail(Long tenantId, Long id) {
        ImportJobEntity job = importJobRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "import job not found"));
        return toDetail(job);
    }

    public ImportJobDetailResponse toDetail(ImportJobEntity job) {
        return new ImportJobDetailResponse(
                job.getId(),
                job.getTenantId(),
                job.getImportType(),
                job.getSourceType(),
                job.getSourceFilename(),
                job.getStorageKey(),
                job.getStatus(),
                job.getRequestedBy(),
                job.getRequestId(),
                job.getTotalCount(),
                job.getSuccessCount(),
                job.getFailureCount(),
                job.getErrorSummary(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getFinishedAt(),
                importJobItemErrorRepository.findAllByTenantIdAndImportJobIdOrderByIdAsc(job.getTenantId(), job.getId())
                        .stream()
                        .map(error -> new ImportJobErrorItemResponse(
                                error.getId(),
                                error.getRowNumber(),
                                error.getErrorCode(),
                                error.getErrorMessage(),
                                error.getRawPayload(),
                                error.getCreatedAt()
                        ))
                        .toList()
        );
    }

    private ImportJobListItemResponse toListItem(ImportJobEntity job) {
        boolean hasFailures = job.getFailureCount() != null && job.getFailureCount() > 0;
        return new ImportJobListItemResponse(
                job.getId(),
                job.getImportType(),
                job.getSourceType(),
                job.getSourceFilename(),
                job.getStatus(),
                job.getRequestedBy(),
                hasFailures,
                job.getTotalCount(),
                job.getSuccessCount(),
                job.getFailureCount(),
                job.getErrorSummary(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getFinishedAt()
        );
    }

    private ImportJobPageQuery normalizeQuery(ImportJobPageQuery query) {
        ImportJobPageQuery normalized = query == null ? new ImportJobPageQuery() : query;
        normalized.setPage(normalizePage(query));
        normalized.setSize(normalizeSize(query));
        normalized.setStatus(normalizeFilter(normalized.getStatus()));
        normalized.setImportType(normalizeFilter(normalized.getImportType()));
        normalized.setHasFailuresOnly(Boolean.TRUE.equals(normalized.getHasFailuresOnly()));
        return normalized;
    }

    private int normalizePage(ImportJobPageQuery query) {
        if (query == null || query.getPage() == null || query.getPage() < 0) {
            return DEFAULT_PAGE;
        }
        return query.getPage();
    }

    private int normalizeSize(ImportJobPageQuery query) {
        if (query == null || query.getSize() == null || query.getSize() <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(query.getSize(), MAX_SIZE);
    }

    private String normalizeFilter(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
