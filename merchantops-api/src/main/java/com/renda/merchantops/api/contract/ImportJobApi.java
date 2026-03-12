package com.renda.merchantops.api.contract;

import com.renda.merchantops.api.dto.importjob.command.ImportJobCreateRequest;
import com.renda.merchantops.api.dto.importjob.query.ImportJobDetailResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobErrorPageQuery;
import com.renda.merchantops.api.dto.importjob.query.ImportJobErrorPageResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobPageQuery;
import com.renda.merchantops.api.dto.importjob.query.ImportJobPageResponse;
import com.renda.merchantops.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Import Jobs")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/import-jobs")
public interface ImportJobApi {

    @Operation(
            summary = "Create async import job",
            description = "Creates a QUEUED import job. Runtime processing stays internal to the worker, runs sequential chunks for USER_CSV, and fails oversized files with MAX_ROWS_EXCEEDED."
    )
    @PostMapping(consumes = {"multipart/form-data"})
    ApiResponse<ImportJobDetailResponse> createImportJob(@Valid @RequestPart("request") ImportJobCreateRequest request,
                                                         @RequestPart("file") MultipartFile file);

    @Operation(summary = "Page import jobs in current tenant with optional queue filters")
    @GetMapping
    ApiResponse<ImportJobPageResponse> listImportJobs(@ParameterObject ImportJobPageQuery query);

    @Operation(
            summary = "Get import job detail in current tenant",
            description = "Detail shape stays stable while totalCount, successCount, and failureCount now advance during PROCESSING after each internal chunk."
    )
    @GetMapping("/{id}")
    ApiResponse<ImportJobDetailResponse> getImportJob(@PathVariable("id") Long id);

    @Operation(
            summary = "Replay failed rows from one terminal import job as a new derived job",
            description = "Creates a derived QUEUED job from replayable failed rows only. Replay jobs use the same standard sequential chunk worker path as any other USER_CSV import."
    )
    @PostMapping("/{id}/replay-failures")
    ApiResponse<ImportJobDetailResponse> replayFailedRows(@PathVariable("id") Long id);

    @Operation(summary = "Page import job errors in current tenant")
    @GetMapping("/{id}/errors")
    ApiResponse<ImportJobErrorPageResponse> listImportJobErrors(@PathVariable("id") Long id,
                                                                @ParameterObject ImportJobErrorPageQuery query);
}
