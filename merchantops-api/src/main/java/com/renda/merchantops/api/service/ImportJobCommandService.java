package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.importjob.command.ImportJobCreateRequest;
import com.renda.merchantops.api.dto.importjob.query.ImportJobDetailResponse;
import com.renda.merchantops.api.messaging.ImportJobMessage;
import com.renda.merchantops.api.messaging.ImportJobPublisher;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.ImportJobEntity;
import com.renda.merchantops.infra.repository.ImportJobRepository;
import com.renda.merchantops.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImportJobCommandService {

    private static final String IMPORT_SOURCE_TYPE_CSV = "CSV";

    private final ImportJobRepository importJobRepository;
    private final ImportFileStorageService importFileStorageService;
    private final ImportJobPublisher importJobPublisher;
    private final ImportJobQueryService importJobQueryService;
    private final AuditEventService auditEventService;
    private final UserRepository userRepository;

    @Transactional
    public ImportJobDetailResponse createJob(Long tenantId,
                                             Long operatorId,
                                             String requestId,
                                             ImportJobCreateRequest request,
                                             MultipartFile file) {
        if (tenantId == null || operatorId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "user context missing");
        }
        if (!StringUtils.hasText(requestId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "request id missing");
        }
        if (userRepository.findByIdAndTenantId(operatorId, tenantId).isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "operator does not belong to tenant");
        }
        String importType = normalizeImportType(request == null ? null : request.getImportType());
        String storageKey = importFileStorageService.store(tenantId, file);

        ImportJobEntity entity = new ImportJobEntity();
        entity.setTenantId(tenantId);
        entity.setImportType(importType);
        entity.setSourceType(IMPORT_SOURCE_TYPE_CSV);
        entity.setSourceFilename(resolveFilename(file));
        entity.setStorageKey(storageKey);
        entity.setStatus("QUEUED");
        entity.setRequestedBy(operatorId);
        entity.setRequestId(requestId);
        entity.setTotalCount(0);
        entity.setSuccessCount(0);
        entity.setFailureCount(0);
        entity.setCreatedAt(LocalDateTime.now());
        ImportJobEntity saved = importJobRepository.save(entity);

        auditEventService.recordEvent(
                tenantId,
                "IMPORT_JOB",
                saved.getId(),
                "IMPORT_JOB_CREATED",
                operatorId,
                requestId,
                null,
                Map.of("status", saved.getStatus(), "importType", saved.getImportType(), "sourceFilename", saved.getSourceFilename())
        );

        importJobPublisher.publish(new ImportJobMessage(saved.getId(), tenantId));

        return importJobQueryService.toDetail(saved);
    }

    private String normalizeImportType(String importType) {
        if (!StringUtils.hasText(importType)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "importType must not be blank");
        }
        String normalized = importType.trim().toUpperCase(Locale.ROOT);
        if (!"USER_CSV".equals(normalized)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "importType must be USER_CSV");
        }
        return normalized;
    }

    private String resolveFilename(MultipartFile file) {
        String filename = file == null ? null : file.getOriginalFilename();
        return StringUtils.hasText(filename) ? filename.trim() : "upload.csv";
    }
}
