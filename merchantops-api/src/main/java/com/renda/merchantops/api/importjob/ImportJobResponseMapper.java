package com.renda.merchantops.api.importjob;

import com.renda.merchantops.api.dto.importjob.query.ImportJobAiInteractionListItemResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobDetailResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobErrorCodeCountResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobErrorItemResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobListItemResponse;
import com.renda.merchantops.domain.importjob.ImportJobAiInteractionItem;
import com.renda.merchantops.domain.importjob.ImportJobDetail;
import com.renda.merchantops.domain.importjob.ImportJobErrorRecord;
import com.renda.merchantops.domain.importjob.ImportJobRecord;
import org.springframework.stereotype.Component;

@Component
class ImportJobResponseMapper {

    ImportJobDetailResponse toDetailResponse(ImportJobDetail detail) {
        ImportJobRecord job = detail.job();
        return new ImportJobDetailResponse(
                job.id(),
                job.tenantId(),
                job.importType(),
                job.sourceType(),
                job.sourceFilename(),
                job.storageKey(),
                job.sourceJobId(),
                job.status(),
                job.requestedBy(),
                job.requestId(),
                job.totalCount(),
                job.successCount(),
                job.failureCount(),
                job.errorSummary(),
                job.createdAt(),
                job.startedAt(),
                job.finishedAt(),
                detail.errorCodeCounts().stream()
                        .map(item -> new ImportJobErrorCodeCountResponse(item.errorCode(), item.count()))
                        .toList(),
                detail.itemErrors().stream().map(this::toErrorItem).toList()
        );
    }

    ImportJobListItemResponse toListItem(ImportJobRecord job) {
        boolean hasFailures = job.failureCount() != null && job.failureCount() > 0;
        return new ImportJobListItemResponse(
                job.id(),
                job.importType(),
                job.sourceType(),
                job.sourceFilename(),
                job.status(),
                job.requestedBy(),
                hasFailures,
                job.totalCount(),
                job.successCount(),
                job.failureCount(),
                job.errorSummary(),
                job.createdAt(),
                job.startedAt(),
                job.finishedAt()
        );
    }

    ImportJobErrorItemResponse toErrorItem(ImportJobErrorRecord error) {
        return new ImportJobErrorItemResponse(
                error.id(),
                error.rowNumber(),
                error.errorCode(),
                error.errorMessage(),
                error.rawPayload(),
                error.createdAt()
        );
    }

    ImportJobAiInteractionListItemResponse toAiInteractionItem(ImportJobAiInteractionItem item) {
        return new ImportJobAiInteractionListItemResponse(
                item.id(),
                item.interactionType(),
                item.status(),
                item.outputSummary(),
                item.promptVersion(),
                item.modelId(),
                item.latencyMs(),
                item.requestId(),
                item.usagePromptTokens(),
                item.usageCompletionTokens(),
                item.usageTotalTokens(),
                item.usageCostMicros(),
                item.createdAt()
        );
    }
}
