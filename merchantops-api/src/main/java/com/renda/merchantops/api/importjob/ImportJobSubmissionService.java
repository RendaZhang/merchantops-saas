package com.renda.merchantops.api.importjob;

import com.renda.merchantops.api.context.RequestIdPolicy;
import com.renda.merchantops.api.dto.importjob.command.ImportJobCreateRequest;
import com.renda.merchantops.api.dto.importjob.query.ImportJobDetailResponse;
import com.renda.merchantops.api.importjob.messaging.ImportJobCreatedEvent;
import com.renda.merchantops.domain.importjob.ImportJobCommandUseCase;
import com.renda.merchantops.domain.importjob.ImportJobRecord;
import com.renda.merchantops.domain.importjob.NewImportJobDraft;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ImportJobSubmissionService {

    private static final String IMPORT_SOURCE_TYPE_CSV = "CSV";

    private final ImportJobCommandUseCase importJobCommandUseCase;
    private final ImportFileStorageService importFileStorageService;
    private final ImportJobOperatorValidator importJobOperatorValidator;
    private final ImportStoredFileCleanupSupport importStoredFileCleanupSupport;
    private final ImportJobQueryService importJobQueryService;
    private final ImportJobAuditService importJobAuditService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public ImportJobDetailResponse createJob(Long tenantId,
                                             Long operatorId,
                                             String requestId,
                                             ImportJobCreateRequest request,
                                             MultipartFile file) {
        String resolvedRequestId = RequestIdPolicy.requireNormalized(requestId);
        importJobOperatorValidator.requireOperatorInTenant(tenantId, operatorId);
        String storageKey = importFileStorageService.store(tenantId, file);
        importStoredFileCleanupSupport.registerRollbackCleanup(storageKey);

        ImportJobRecord saved = importJobCommandUseCase.createQueuedJob(
                tenantId,
                operatorId,
                resolvedRequestId,
                new NewImportJobDraft(
                        request == null ? null : request.getImportType(),
                        IMPORT_SOURCE_TYPE_CSV,
                        resolveFilename(file),
                        storageKey,
                        null
                )
        );

        importJobAuditService.recordImportJobCreatedEvent(saved, operatorId, resolvedRequestId, null);
        applicationEventPublisher.publishEvent(new ImportJobCreatedEvent(saved.id(), tenantId));
        return importJobQueryService.getJobDetail(tenantId, saved.id());
    }

    private String resolveFilename(MultipartFile file) {
        String filename = file == null ? null : file.getOriginalFilename();
        return StringUtils.hasText(filename) ? filename.trim() : "upload.csv";
    }
}
