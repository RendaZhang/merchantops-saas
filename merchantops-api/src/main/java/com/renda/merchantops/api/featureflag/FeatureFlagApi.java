package com.renda.merchantops.api.featureflag;

import com.renda.merchantops.api.dto.featureflag.command.FeatureFlagUpdateRequest;
import com.renda.merchantops.api.dto.featureflag.query.FeatureFlagItemResponse;
import com.renda.merchantops.api.dto.featureflag.query.FeatureFlagListResponse;
import com.renda.merchantops.api.platform.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.renda.merchantops.api.doc.OpenApiExamples.REQ_FEATURE_FLAG_UPDATE;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_BAD_REQUEST_FEATURE_FLAG_ENABLED_NULL;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_FEATURE_FLAG_ITEM;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_FEATURE_FLAG_LIST;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_FORBIDDEN;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_UNAUTHORIZED;

@Tag(name = "Feature Flags")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/feature-flags")
public interface FeatureFlagApi {

    @Operation(summary = "List real feature flags in stable key order")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Feature flags returned",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_FEATURE_FLAG_LIST))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_UNAUTHORIZED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Missing FEATURE_FLAG_MANAGE permission",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_FORBIDDEN))
            )
    })
    @GetMapping
    ApiResponse<FeatureFlagListResponse> listFeatureFlags();

    @Operation(summary = "Update one real feature flag by key")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = REQ_FEATURE_FLAG_UPDATE))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Feature flag updated or returned unchanged",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_FEATURE_FLAG_ITEM))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_UNAUTHORIZED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Missing FEATURE_FLAG_MANAGE permission",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_FORBIDDEN))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "enabled must not be null",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_BAD_REQUEST_FEATURE_FLAG_ENABLED_NULL))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Feature flag not found",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            value = "{\"code\":\"NOT_FOUND\",\"message\":\"feature flag not found\",\"data\":null}"
                    ))
            )
    })
    @PutMapping("/{key}")
    ApiResponse<FeatureFlagItemResponse> updateFeatureFlag(@PathVariable("key") String key,
                                                           @Valid @RequestBody FeatureFlagUpdateRequest request);
}
