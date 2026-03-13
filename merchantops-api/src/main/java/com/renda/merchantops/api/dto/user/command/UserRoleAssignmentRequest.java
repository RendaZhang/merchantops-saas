package com.renda.merchantops.api.dto.user.command;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Replace user roles within current tenant")
public class UserRoleAssignmentRequest {

    @NotEmpty(message = "roleCodes must not be empty")
    @ArraySchema(schema = @Schema(description = "Tenant-local role code", example = "READ_ONLY"))
    private List<String> roleCodes;
}
