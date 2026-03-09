package com.renda.merchantops.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
    @Schema(description = "Password", example = "123456")
    private String password;

}
