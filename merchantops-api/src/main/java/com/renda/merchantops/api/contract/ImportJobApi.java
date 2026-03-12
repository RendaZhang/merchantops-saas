package com.renda.merchantops.api.contract;

import com.renda.merchantops.api.dto.importjob.command.ImportJobCreateRequest;
import com.renda.merchantops.api.dto.importjob.query.ImportJobDetailResponse;
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

    @Operation(summary = "Create async import job")
    @PostMapping(consumes = {"multipart/form-data"})
    ApiResponse<ImportJobDetailResponse> createImportJob(@Valid @RequestPart("request") ImportJobCreateRequest request,
                                                         @RequestPart("file") MultipartFile file);

    @Operation(summary = "Page import jobs in current tenant")
    @GetMapping
    ApiResponse<ImportJobPageResponse> listImportJobs(@ParameterObject ImportJobPageQuery query);

    @Operation(summary = "Get import job detail in current tenant")
    @GetMapping("/{id}")
    ApiResponse<ImportJobDetailResponse> getImportJob(@PathVariable("id") Long id);
}
