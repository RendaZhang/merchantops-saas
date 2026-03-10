package com.renda.merchantops.api.contract;

import com.renda.merchantops.api.dto.user.command.UserCreateRequest;
import com.renda.merchantops.api.dto.user.command.UserCreateResponse;
import com.renda.merchantops.api.dto.user.query.UserPageQuery;
import com.renda.merchantops.api.dto.user.query.UserPageResponse;
import com.renda.merchantops.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.renda.merchantops.api.doc.OpenApiExamples.REQ_USER_CREATE;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_BAD_REQUEST_ROLE_CODES;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_BAD_REQUEST_USERNAME_EXISTS;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_FORBIDDEN;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_UNAUTHORIZED;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_USER_CREATED;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_USER_LIST;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_VALIDATION_ERROR_PASSWORD_WHITESPACE;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_VALIDATION_ERROR_USER_CREATE;

@Tag(name = "User Management")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/users")
public interface UserManagementApi {

    @Operation(
            summary = "Page users in current tenant",
            description = "Requires USER_READ permission. Supports page/size and optional username, status, roleCode filters. The tenant scope is derived from JWT, not request parameters."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Query successful",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_USER_LIST))
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
    @GetMapping
    ApiResponse<UserPageResponse> listUsers(@ParameterObject UserPageQuery query);

    @Operation(
            summary = "Create user in current tenant",
            description = "Requires USER_WRITE permission. The new user is always created as ACTIVE, the password is stored as BCrypt, and every requested roleCode must belong to the current tenant."
    )
    @RequestBody(
            required = true,
            description = "User create payload",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = REQ_USER_CREATE))
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Create successful",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_USER_CREATED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation failed or role/username rule violated",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "validationError", value = RESP_VALIDATION_ERROR_USER_CREATE),
                            @ExampleObject(name = "passwordWhitespace", value = RESP_VALIDATION_ERROR_PASSWORD_WHITESPACE),
                            @ExampleObject(name = "duplicateUsername", value = RESP_BAD_REQUEST_USERNAME_EXISTS),
                            @ExampleObject(name = "invalidRoleCodes", value = RESP_BAD_REQUEST_ROLE_CODES)
                    })
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_UNAUTHORIZED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Missing USER_WRITE permission",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_FORBIDDEN))
            )
    })
    @PostMapping
    ApiResponse<UserCreateResponse> createUser(@Valid @org.springframework.web.bind.annotation.RequestBody UserCreateRequest request);
}
