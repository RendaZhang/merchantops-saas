package com.renda.merchantops.api.importjob;

import com.renda.merchantops.api.dto.importjob.query.ImportJobAiErrorSummaryResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobAiFixRecommendationResponse;
import com.renda.merchantops.api.dto.importjob.query.ImportJobAiMappingSuggestionResponse;
import com.renda.merchantops.api.platform.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_BAD_REQUEST_IMPORT_AI_MAPPING_SUGGESTION_NO_FAILURE_SIGNAL;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_BAD_REQUEST_IMPORT_AI_MAPPING_SUGGESTION_NO_HEADER_SIGNAL;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_BAD_REQUEST_IMPORT_AI_FIX_RECOMMENDATION_NO_FAILURE_SIGNAL;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_BAD_REQUEST_IMPORT_AI_FIX_RECOMMENDATION_NO_ROW_SIGNAL;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_BAD_REQUEST_IMPORT_AI_FIX_RECOMMENDATION_UNSUPPORTED_IMPORT_TYPE;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_FORBIDDEN;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_IMPORT_JOB_AI_ERROR_SUMMARY;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_IMPORT_JOB_AI_FIX_RECOMMENDATION;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_IMPORT_JOB_AI_MAPPING_SUGGESTION;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_SERVICE_UNAVAILABLE_IMPORT_AI_ERROR_SUMMARY_DISABLED;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_SERVICE_UNAVAILABLE_IMPORT_AI_ERROR_SUMMARY_PROVIDER;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_SERVICE_UNAVAILABLE_IMPORT_AI_FIX_RECOMMENDATION_DISABLED;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_SERVICE_UNAVAILABLE_IMPORT_AI_FIX_RECOMMENDATION_PROVIDER;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_SERVICE_UNAVAILABLE_IMPORT_AI_MAPPING_SUGGESTION_DISABLED;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_SERVICE_UNAVAILABLE_IMPORT_AI_MAPPING_SUGGESTION_PROVIDER;
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

    @Operation(
            summary = "Generate AI mapping suggestion for one current-tenant import job",
            description = "Builds a read-only, suggestion-only mapping proposal from import job detail, error-code counts, sanitized header or global error signals, and the first 20 sanitized row-level failures without sending raw CSV payload values to the model."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "AI mapping suggestion generated",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_IMPORT_JOB_AI_MAPPING_SUGGESTION))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Import job is not eligible for mapping suggestion",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "noFailureSignal", value = RESP_BAD_REQUEST_IMPORT_AI_MAPPING_SUGGESTION_NO_FAILURE_SIGNAL),
                            @ExampleObject(name = "noHeaderSignal", value = RESP_BAD_REQUEST_IMPORT_AI_MAPPING_SUGGESTION_NO_HEADER_SIGNAL)
                    })
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
                            @ExampleObject(name = "featureDisabled", value = RESP_SERVICE_UNAVAILABLE_IMPORT_AI_MAPPING_SUGGESTION_DISABLED),
                            @ExampleObject(name = "providerUnavailable", value = RESP_SERVICE_UNAVAILABLE_IMPORT_AI_MAPPING_SUGGESTION_PROVIDER)
                    })
            )
    })
    @PostMapping("/{id}/ai-mapping-suggestion")
    ApiResponse<ImportJobAiMappingSuggestionResponse> getAiMappingSuggestion(@PathVariable("id") Long id);

    @Operation(
            summary = "Generate AI fix recommendation for one current-tenant import job",
            description = "Builds a read-only, suggestion-only remediation plan from current import job detail, error-code counts, and grounded row-level sanitized failure groups without sending raw CSV payload values or replacement values to the model."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "AI fix recommendation generated",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_IMPORT_JOB_AI_FIX_RECOMMENDATION))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Import job is not eligible for fix recommendation",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "noFailureSignal", value = RESP_BAD_REQUEST_IMPORT_AI_FIX_RECOMMENDATION_NO_FAILURE_SIGNAL),
                            @ExampleObject(name = "unsupportedImportType", value = RESP_BAD_REQUEST_IMPORT_AI_FIX_RECOMMENDATION_UNSUPPORTED_IMPORT_TYPE),
                            @ExampleObject(name = "noRowSignal", value = RESP_BAD_REQUEST_IMPORT_AI_FIX_RECOMMENDATION_NO_ROW_SIGNAL)
                    })
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
                            @ExampleObject(name = "featureDisabled", value = RESP_SERVICE_UNAVAILABLE_IMPORT_AI_FIX_RECOMMENDATION_DISABLED),
                            @ExampleObject(name = "providerUnavailable", value = RESP_SERVICE_UNAVAILABLE_IMPORT_AI_FIX_RECOMMENDATION_PROVIDER)
                    })
            )
    })
    @PostMapping("/{id}/ai-fix-recommendation")
    ApiResponse<ImportJobAiFixRecommendationResponse> getAiFixRecommendation(@PathVariable("id") Long id);
}
