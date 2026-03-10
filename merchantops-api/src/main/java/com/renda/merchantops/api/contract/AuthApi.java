package com.renda.merchantops.api.contract;

import com.renda.merchantops.api.dto.auth.LoginRequest;
import com.renda.merchantops.api.dto.auth.LoginResponse;
import com.renda.merchantops.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.renda.merchantops.api.doc.OpenApiExamples.LOGIN_REQUEST_ADMIN;
import static com.renda.merchantops.api.doc.OpenApiExamples.LOGIN_REQUEST_OPS;
import static com.renda.merchantops.api.doc.OpenApiExamples.LOGIN_REQUEST_VIEWER;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_BAD_REQUEST_CREDENTIAL;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_FORBIDDEN_USER_INACTIVE;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_SUCCESS_LOGIN;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_VALIDATION_ERROR_PASSWORD_WHITESPACE;

@Tag(name = "Authentication")
@RequestMapping("/api/v1/auth")
public interface AuthApi {

    @Operation(
            summary = "Login and get JWT access token",
            description = "Authenticate by tenantCode + username + password. Use one of the built-in demo accounts from examples."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Tenant-scoped login credentials",
            content = @Content(
                    schema = @Schema(implementation = LoginRequest.class),
                    examples = {
                            @ExampleObject(name = "admin-demo", summary = "Tenant admin account", value = LOGIN_REQUEST_ADMIN),
                            @ExampleObject(name = "ops-demo", summary = "Operations account", value = LOGIN_REQUEST_OPS),
                            @ExampleObject(name = "viewer-demo", summary = "Read-only account", value = LOGIN_REQUEST_VIEWER)
                    }
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_SUCCESS_LOGIN))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or wrong credentials",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "wrongCredentials", value = RESP_BAD_REQUEST_CREDENTIAL),
                            @ExampleObject(name = "passwordWhitespace", value = RESP_VALIDATION_ERROR_PASSWORD_WHITESPACE)
                    })
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Tenant or user is not active",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_FORBIDDEN_USER_INACTIVE))
            )
    })
    @PostMapping("/login")
    ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request);
}
