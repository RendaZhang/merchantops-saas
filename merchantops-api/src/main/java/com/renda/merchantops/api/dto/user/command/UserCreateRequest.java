package com.renda.merchantops.api.dto.user.command;

import com.renda.merchantops.api.validation.PasswordRules;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create user request within current tenant")
public class UserCreateRequest {

    @NotBlank(message = "username must not be blank")
    @Size(max = 64, message = "username length must be less than or equal to 64")
    @Schema(description = "Unique username within current tenant", example = "cashier")
    private String username;

    @NotBlank(message = "displayName must not be blank")
    @Size(max = 128, message = "displayName length must be less than or equal to 128")
    @Schema(description = "Display name", example = "Cashier User")
    private String displayName;

    @Email(message = "email must be a well-formed email address")
    @Size(max = 128, message = "email length must be less than or equal to 128")
    @Schema(description = "Email address", example = "cashier@demo-shop.local")
    private String email;

    @NotBlank(message = "password must not be blank")
    @Pattern(
            regexp = PasswordRules.NO_BOUNDARY_WHITESPACE_REGEX,
            message = PasswordRules.NO_BOUNDARY_WHITESPACE_MESSAGE
    )
    @Size(max = 128, message = "password length must be less than or equal to 128")
    @Schema(description = "Raw password; server stores BCrypt hash. Internal spaces are allowed, but leading and trailing whitespace are rejected.", example = "123456")
    private String password;

    @NotEmpty(message = "roleCodes must not be empty")
    @ArraySchema(schema = @Schema(description = "Role code in current tenant", example = "READ_ONLY"))
    private List<@NotBlank(message = "roleCodes must not contain blank values") String> roleCodes;
}
