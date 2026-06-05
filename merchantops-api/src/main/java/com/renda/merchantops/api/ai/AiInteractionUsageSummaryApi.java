package com.renda.merchantops.api.ai;

import com.renda.merchantops.api.dto.ai.query.AiInteractionUsageSummaryQuery;
import com.renda.merchantops.api.dto.ai.query.AiInteractionUsageSummaryResponse;
import com.renda.merchantops.api.platform.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_AI_INTERACTION_USAGE_SUMMARY;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_FORBIDDEN;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_UNAUTHORIZED;

@Tag(name = "AI Governance")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/ai-interactions")
public interface AiInteractionUsageSummaryApi {

    @Operation(summary = "Summarize tenant-scoped AI usage and cost from stored interaction metadata")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Tenant-scoped AI usage summary returned",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_AI_INTERACTION_USAGE_SUMMARY))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid query range or unsupported entity type",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(
                            value = "{\"code\":\"BAD_REQUEST\",\"message\":\"from must be before or equal to to\",\"data\":null}"
                    ))
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
            )
    })
    @GetMapping("/usage-summary")
    ApiResponse<AiInteractionUsageSummaryResponse> getUsageSummary(@ParameterObject AiInteractionUsageSummaryQuery query);
}
