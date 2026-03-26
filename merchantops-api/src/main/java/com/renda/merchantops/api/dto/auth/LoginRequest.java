package com.renda.merchantops.api.dto.auth;

import com.renda.merchantops.domain.auth.PasswordPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Login request with tenant scope")
public class LoginRequest {

    @NotBlank(message = "tenantCode must not be blank")
    @Schema(description = "Tenant code", example = "demo-shop")
    private String tenantCode;

    @NotBlank(message = "username must not be blank")
    @Schema(description = "Username under tenant", example = "admin")
    private String username;

    @NotBlank(message = "password must not be blank")
    @Pattern(
            regexp = PasswordPolicy.NO_BOUNDARY_WHITESPACE_REGEX,
            message = PasswordPolicy.NO_BOUNDARY_WHITESPACE_MESSAGE
    )
    @Schema(description = "Password; must not start or end with whitespace", example = "123456")
    private String password;

}
