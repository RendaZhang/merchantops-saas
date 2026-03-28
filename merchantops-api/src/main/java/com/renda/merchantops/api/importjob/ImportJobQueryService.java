package com.renda.merchantops.api.importjob;

import com.renda.merchantops.api.dto.importjob.query.ImportJobAiInteractionPageQuery;
import com.renda.merchantops.api.dto.importjob.query.ImportJobAiInteractionPageResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobDetailResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobErrorPageQuery;
import com.renda.merchantops.api.dto.importjob.query.ImportJobErrorPageResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobPageQuery;
import com.renda.merchantops.api.dto.importjob.query.ImportJobPageResponse;
import com.renda.merchantops.domain.importjob.ImportJobAiInteractionPageCriteria;
import com.renda.merchantops.domain.importjob.ImportJobErrorPageCriteria;
import com.renda.merchantops.domain.importjob.ImportJobQueryUseCase;
import com.renda.merchantops.domain.importjob.ImportJobPageCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImportJobQueryService {

    private final ImportJobQueryUseCase importJobQueryUseCase;
    private final ImportJobResponseMapper importJobResponseMapper;

    public ImportJobPageResponse pageJobs(Long tenantId, ImportJobPageQuery query) {
        var result = importJobQueryUseCase.pageJobs(tenantId, toCriteria(query));
        return new ImportJobPageResponse(
                result.items().stream().map(importJobResponseMapper::toListItem).toList(),
                result.page(),
                result.size(),
                result.total(),
                result.totalPages()
        );
    }

    public ImportJobDetailResponse getJobDetail(Long tenantId, Long id) {
        return importJobResponseMapper.toDetailResponse(importJobQueryUseCase.getJobDetail(tenantId, id));
    }

    public ImportJobAiInteractionPageResponse pageJobAiInteractions(Long tenantId,
                                                                    Long importJobId,
                                                                    ImportJobAiInteractionPageQuery query) {
        var result = importJobQueryUseCase.pageJobAiInteractions(tenantId, importJobId, toAiInteractionCriteria(query));
        return new ImportJobAiInteractionPageResponse(
                result.items().stream().map(importJobResponseMapper::toAiInteractionItem).toList(),
                result.page(),
                result.size(),
                result.total(),
                result.totalPages()
        );
    }

    public ImportJobErrorPageResponse pageJobErrors(Long tenantId, Long importJobId, ImportJobErrorPageQuery query) {
        var result = importJobQueryUseCase.pageJobErrors(tenantId, importJobId, toErrorCriteria(query));
        return new ImportJobErrorPageResponse(
                result.items().stream().map(importJobResponseMapper::toErrorItem).toList(),
                result.page(),
                result.size(),
                result.total(),
                result.totalPages()
        );
    }

    private ImportJobPageCriteria toCriteria(ImportJobPageQuery query) {
        if (query == null) {
            return null;
        }
        return new ImportJobPageCriteria(
                query.getPage() == null ? -1 : query.getPage(),
                query.getSize() == null ? 0 : query.getSize(),
                query.getStatus(),
                query.getImportType(),
                query.getRequestedBy(),
                Boolean.TRUE.equals(query.getHasFailuresOnly())
        );
    }

    private ImportJobAiInteractionPageCriteria toAiInteractionCriteria(ImportJobAiInteractionPageQuery query) {
        if (query == null) {
            return null;
        }
        return new ImportJobAiInteractionPageCriteria(
                query.getPage() == null ? -1 : query.getPage(),
                query.getSize() == null ? 0 : query.getSize(),
                query.getInteractionType(),
                query.getStatus()
        );
    }

    private ImportJobErrorPageCriteria toErrorCriteria(ImportJobErrorPageQuery query) {
        if (query == null) {
            return null;
        }
        return new ImportJobErrorPageCriteria(
                query.getPage() == null ? -1 : query.getPage(),
                query.getSize() == null ? 0 : query.getSize(),
                query.getErrorCode()
        );
    }
}
