package com.renda.merchantops.api.dto.user.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update user status within current tenant")
public class UserStatusUpdateRequest {

    @NotBlank(message = "status must not be blank")
    @Pattern(regexp = "ACTIVE|DISABLED", message = "status must be one of ACTIVE, DISABLED")
    @Schema(description = "User status", allowableValues = {"ACTIVE", "DISABLED"}, example = "DISABLED")
    private String status;
}
