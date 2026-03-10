package com.renda.merchantops.api.contract;

import com.renda.merchantops.api.dto.user.command.UserCreateRequest;
import com.renda.merchantops.api.dto.user.command.UserCreateResponse;
import com.renda.merchantops.api.dto.user.command.UserRoleAssignmentRequest;
import com.renda.merchantops.api.dto.user.command.UserRoleAssignmentResponse;
import com.renda.merchantops.api.dto.user.command.UserStatusUpdateRequest;
import com.renda.merchantops.api.dto.user.command.UserUpdateRequest;
import com.renda.merchantops.api.dto.user.command.UserWriteResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.renda.merchantops.api.doc.OpenApiExamples.REQ_USER_CREATE;
import static com.renda.merchantops.api.doc.OpenApiExamples.REQ_USER_ROLE_ASSIGNMENT;
import static com.renda.merchantops.api.doc.OpenApiExamples.REQ_USER_STATUS_UPDATE;
import static com.renda.merchantops.api.doc.OpenApiExamples.REQ_USER_UPDATE;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_BAD_REQUEST_ROLE_CODES;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_BAD_REQUEST_USERNAME_EXISTS;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_FORBIDDEN;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_UNAUTHORIZED;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_USER_CREATED;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_USER_LIST;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_USER_PROFILE_UPDATED;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_USER_ROLES_UPDATED;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_USER_STATUS_UPDATED;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_VALIDATION_ERROR_PASSWORD_WHITESPACE;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_VALIDATION_ERROR_ROLE_ASSIGNMENT;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_VALIDATION_ERROR_STATUS_UPDATE;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_VALIDATION_ERROR_USER_CREATE;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_VALIDATION_ERROR_USER_UPDATE;

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

    @Operation(
            summary = "Update mutable user profile fields in current tenant",
            description = "Requires USER_WRITE permission. Only displayName and email are mutable here. tenantId, username, and passwordHash are not public update fields."
    )
    @RequestBody(
            required = true,
            description = "User profile update payload",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = REQ_USER_UPDATE))
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Update successful",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_USER_PROFILE_UPDATED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation failed",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_VALIDATION_ERROR_USER_UPDATE))
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found in current tenant",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":\"NOT_FOUND\",\"message\":\"user not found\",\"data\":null}"))
            )
    })
    @PutMapping("/{id}")
    ApiResponse<UserWriteResponse> updateUser(@PathVariable("id") Long id,
                                              @Valid @org.springframework.web.bind.annotation.RequestBody UserUpdateRequest request);

    @Operation(
            summary = "Update user status in current tenant",
            description = "Requires USER_WRITE permission. Only ACTIVE and DISABLED are allowed. Use this endpoint for account enable/disable rather than the general profile update endpoint."
    )
    @RequestBody(
            required = true,
            description = "User status update payload",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = REQ_USER_STATUS_UPDATE))
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Status update successful",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_USER_STATUS_UPDATED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation failed",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_VALIDATION_ERROR_STATUS_UPDATE))
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found in current tenant",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":\"NOT_FOUND\",\"message\":\"user not found\",\"data\":null}"))
            )
    })
    @PatchMapping("/{id}/status")
    ApiResponse<UserWriteResponse> updateUserStatus(@PathVariable("id") Long id,
                                                    @Valid @org.springframework.web.bind.annotation.RequestBody UserStatusUpdateRequest request);

    @Operation(
            summary = "Replace user roles in current tenant",
            description = "Requires USER_WRITE permission. Replaces all existing roles for the target tenant user. Every roleCode must belong to the current tenant. Old JWT claims become stale after this change, so the user must login again to get a new token."
    )
    @RequestBody(
            required = true,
            description = "User role replacement payload",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = REQ_USER_ROLE_ASSIGNMENT))
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Role assignment successful",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_USER_ROLES_UPDATED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation failed or role scope violated",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "validationError", value = RESP_VALIDATION_ERROR_ROLE_ASSIGNMENT),
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found in current tenant",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"code\":\"NOT_FOUND\",\"message\":\"user not found\",\"data\":null}"))
            )
    })
    @PutMapping("/{id}/roles")
    ApiResponse<UserRoleAssignmentResponse> assignRoles(@PathVariable("id") Long id,
                                                        @Valid @org.springframework.web.bind.annotation.RequestBody UserRoleAssignmentRequest request);
}
