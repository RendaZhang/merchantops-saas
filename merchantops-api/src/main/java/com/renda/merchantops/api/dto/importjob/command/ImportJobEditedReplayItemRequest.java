package com.renda.merchantops.api.dto.importjob.command;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Full replacement row for one edited failed-row replay item")
public class ImportJobEditedReplayItemRequest {

    @NotNull(message = "errorId must not be null")
    @Schema(description = "Import error id to replace", example = "701")
    private Long errorId;

    @NotBlank(message = "username must not be blank")
    @Schema(description = "Replacement username", example = "retry-user")
    private String username;

    @NotBlank(message = "displayName must not be blank")
    @Schema(description = "Replacement display name", example = "Retry User")
    private String displayName;

    @NotBlank(message = "email must not be blank")
    @Schema(description = "Replacement email", example = "retry-user@demo-shop.local")
    private String email;

    @NotBlank(message = "password must not be blank")
    @Schema(description = "Replacement password", example = "123456")
    private String password;

    @NotEmpty(message = "roleCodes must not be empty")
    @ArraySchema(schema = @Schema(description = "Replacement role code", example = "READ_ONLY"))
    private List<@NotBlank(message = "roleCodes must not contain blank values") String> roleCodes;
}
