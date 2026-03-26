package com.renda.merchantops.api.importjob;

import com.renda.merchantops.api.context.ContextAccess;
import com.renda.merchantops.api.context.RequestIdAccess;
import com.renda.merchantops.api.dto.importjob.command.ImportJobCreateRequest;
import com.renda.merchantops.api.dto.importjob.command.ImportJobEditedReplayRequest;
import com.renda.merchantops.api.dto.importjob.command.ImportJobSelectiveReplayRequest;
import com.renda.merchantops.api.dto.importjob.query.ImportJobDetailResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobErrorPageQuery;
import com.renda.merchantops.api.dto.importjob.query.ImportJobErrorPageResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobPageQuery;
import com.renda.merchantops.api.dto.importjob.query.ImportJobPageResponse;
import com.renda.merchantops.api.platform.response.ApiResponse;
import com.renda.merchantops.api.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class ImportJobController implements ImportJobApi {

    private final ImportJobSubmissionService importJobSubmissionService;
    private final ImportJobReplayService importJobReplayService;
    private final ImportJobQueryService importJobQueryService;

    @Override
    @RequirePermission("USER_WRITE")
    public ApiResponse<ImportJobDetailResponse> createImportJob(@Valid @RequestPart("request") ImportJobCreateRequest request,
                                                                @RequestPart("file") MultipartFile file) {
        Long tenantId = ContextAccess.requireTenantId();
        Long operatorId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        return ApiResponse.success(importJobSubmissionService.createJob(tenantId, operatorId, requestId, request, file));
    }

    @Override
    @RequirePermission("USER_READ")
    public ApiResponse<ImportJobPageResponse> listImportJobs(ImportJobPageQuery query) {
        Long tenantId = ContextAccess.requireTenantId();
        return ApiResponse.success(importJobQueryService.pageJobs(tenantId, query));
    }

    @Override
    @RequirePermission("USER_READ")
    public ApiResponse<ImportJobDetailResponse> getImportJob(@PathVariable("id") Long id) {
        Long tenantId = ContextAccess.requireTenantId();
        return ApiResponse.success(importJobQueryService.getJobDetail(tenantId, id));
    }

    @Override
    @RequirePermission("USER_WRITE")
    public ApiResponse<ImportJobDetailResponse> replayFailedRows(@PathVariable("id") Long id) {
        Long tenantId = ContextAccess.requireTenantId();
        Long operatorId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        return ApiResponse.success(importJobReplayService.replayFailedRows(tenantId, operatorId, requestId, id));
    }

    @Override
    @RequirePermission("USER_WRITE")
    public ApiResponse<ImportJobDetailResponse> replayWholeFile(@PathVariable("id") Long id) {
        Long tenantId = ContextAccess.requireTenantId();
        Long operatorId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        return ApiResponse.success(importJobReplayService.replayWholeFile(tenantId, operatorId, requestId, id));
    }

    @Override
    @RequirePermission("USER_WRITE")
    public ApiResponse<ImportJobDetailResponse> replayFailedRowsSelective(@PathVariable("id") Long id,
                                                                          @Valid @RequestBody ImportJobSelectiveReplayRequest request) {
        Long tenantId = ContextAccess.requireTenantId();
        Long operatorId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        return ApiResponse.success(importJobReplayService.replayFailedRowsSelective(tenantId, operatorId, requestId, id, request));
    }

    @Override
    @RequirePermission("USER_WRITE")
    public ApiResponse<ImportJobDetailResponse> replayFailedRowsEdited(@PathVariable("id") Long id,
                                                                       @Valid @RequestBody ImportJobEditedReplayRequest request) {
        Long tenantId = ContextAccess.requireTenantId();
        Long operatorId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        return ApiResponse.success(importJobReplayService.replayFailedRowsEdited(tenantId, operatorId, requestId, id, request));
    }

    @Override
    @RequirePermission("USER_READ")
    public ApiResponse<ImportJobErrorPageResponse> listImportJobErrors(@PathVariable("id") Long id,
                                                                       ImportJobErrorPageQuery query) {
        Long tenantId = ContextAccess.requireTenantId();
        return ApiResponse.success(importJobQueryService.pageJobErrors(tenantId, id, query));
    }
}
