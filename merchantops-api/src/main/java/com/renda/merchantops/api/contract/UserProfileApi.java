package com.renda.merchantops.api.contract;

import com.renda.merchantops.api.dto.user.UserProfileResponse;
import com.renda.merchantops.api.security.CurrentUser;
import com.renda.merchantops.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_UNAUTHORIZED;
import static com.renda.merchantops.api.doc.OpenApiExamples.RESP_USER_PROFILE;

@Tag(name = "User Profile")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/user")
public interface UserProfileApi {

    @Operation(
            summary = "Get current logged-in user profile",
            description = "Returns tenant/user identity and JWT-resolved roles/permissions."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Profile query successful",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_USER_PROFILE))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RESP_UNAUTHORIZED))
            )
    })
    @GetMapping("/me")
    ApiResponse<UserProfileResponse> me(@Parameter(hidden = true) @AuthenticationPrincipal CurrentUser currentUser);
}
