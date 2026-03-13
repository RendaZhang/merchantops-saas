package com.renda.merchantops.api.contract;

import com.renda.merchantops.api.dto.context.ContextResponse;
import com.renda.merchantops.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;

import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_CONTEXT;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_UNAUTHORIZED;

@Tag(name = "Context")
@SecurityRequirement(name = "bearerAuth")
public interface ContextApi {

    @Operation(
            summary = "Get current tenant and user context",
            description = "Reads tenant and user identity from request context restored by JWT filter."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Context query successful",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_CONTEXT))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_UNAUTHORIZED))
            )
    })
    @GetMapping("/api/v1/context")
    ApiResponse<ContextResponse> context();
}
