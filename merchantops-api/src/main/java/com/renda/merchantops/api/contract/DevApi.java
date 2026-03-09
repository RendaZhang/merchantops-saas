package com.renda.merchantops.api.contract;

import com.renda.merchantops.api.dto.EchoRequest;
import com.renda.merchantops.api.dto.dev.DevPingResponse;
import com.renda.merchantops.api.dto.dev.EchoResponse;
import com.renda.merchantops.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_BIZ_ERROR;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_DEV_ECHO;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_DEV_PING;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_VALIDATION_ERROR_ECHO;
import static com.renda.merchantops.api.doc.OpenApiExamples.REQ_DEV_ECHO;

@Tag(name = "Development")
@RequestMapping("/api/v1/dev")
public interface DevApi {

    @Operation(summary = "Ping development endpoint", description = "Public test endpoint for quick connectivity checks.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Ping successful",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_DEV_PING))
            )
    })
    @GetMapping("/ping")
    ApiResponse<DevPingResponse> ping();

    @Operation(summary = "Echo request body", description = "Public test endpoint that returns the same message.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = EchoRequest.class),
                    examples = @ExampleObject(name = "echo-demo", value = REQ_DEV_ECHO)
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Echo successful",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_DEV_ECHO))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation failed",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_VALIDATION_ERROR_ECHO))
            )
    })
    @PostMapping("/echo")
    ApiResponse<EchoResponse> echo(@Valid @RequestBody EchoRequest request);

    @Operation(summary = "Trigger business exception", description = "Public test endpoint for standardized error response shape.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Business exception raised intentionally",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_BIZ_ERROR))
            )
    })
    @GetMapping("/biz-error")
    ApiResponse<Void> bizError();
}
