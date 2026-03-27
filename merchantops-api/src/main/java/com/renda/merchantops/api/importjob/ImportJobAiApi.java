package com.renda.merchantops.api.importjob;

import com.renda.merchantops.api.dto.importjob.query.ImportJobAiErrorSummaryResponse;
import com.renda.merchantops.api.platform.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_FORBIDDEN;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_IMPORT_JOB_AI_ERROR_SUMMARY;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_SERVICE_UNAVAILABLE_IMPORT_AI_ERROR_SUMMARY_DISABLED;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_SERVICE_UNAVAILABLE_IMPORT_AI_ERROR_SUMMARY_PROVIDER;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_UNAUTHORIZED;

@Tag(name = "Import Jobs")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/import-jobs")
public interface ImportJobAiApi {

    @Operation(
            summary = "Generate AI error summary for one current-tenant import job",
            description = "Builds a read-only, suggestion-only summary from import job detail, error-code counts, and the first 20 sanitized failure rows without sending raw CSV payload values to the model."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "AI error summary generated",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_IMPORT_JOB_AI_ERROR_SUMMARY))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_UNAUTHORIZED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Missing USER_READ permission",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_FORBIDDEN))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Import job not found in current tenant",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":\"NOT_FOUND\",\"message\":\"import job not found\",\"data\":null}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "AI feature disabled or provider unavailable",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "featureDisabled", value = RESP_SERVICE_UNAVAILABLE_IMPORT_AI_ERROR_SUMMARY_DISABLED),
                            @ExampleObject(name = "providerUnavailable", value = RESP_SERVICE_UNAVAILABLE_IMPORT_AI_ERROR_SUMMARY_PROVIDER)
                    })
            )
    })
    @PostMapping("/{id}/ai-error-summary")
    ApiResponse<ImportJobAiErrorSummaryResponse> getAiErrorSummary(@PathVariable("id") Long id);
}
