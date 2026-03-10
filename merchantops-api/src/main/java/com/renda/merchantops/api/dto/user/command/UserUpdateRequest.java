package com.renda.merchantops.api.dto.user.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update mutable user profile fields within current tenant")
public class UserUpdateRequest {

    @NotBlank(message = "displayName must not be blank")
    @Size(max = 128, message = "displayName length must be less than or equal to 128")
    @Schema(description = "Display name", example = "Updated Cashier")
    private String displayName;

    @Email(message = "email must be a well-formed email address")
    @Size(max = 128, message = "email length must be less than or equal to 128")
    @Schema(description = "Email address; blank clears the value", example = "cashier.updated@demo-shop.local")
    private String email;
}
