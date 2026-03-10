package com.renda.merchantops.api.contract;

import com.renda.merchantops.api.dto.user.query.UserPageQuery;
import com.renda.merchantops.api.dto.user.query.UserPageResponse;
import com.renda.merchantops.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_FORBIDDEN;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_UNAUTHORIZED;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_USER_LIST;

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
}
