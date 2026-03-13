package com.renda.merchantops.api.contract;

import com.renda.merchantops.api.dto.health.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;

import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_HEALTH;

@Tag(name = "Health")
public interface HealthApi {

    @Operation(summary = "Service health check", description = "Lightweight health endpoint exposed without authentication.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Service healthy",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_HEALTH))
            )
    })
    @GetMapping("/health")
    HealthResponse health();
}
