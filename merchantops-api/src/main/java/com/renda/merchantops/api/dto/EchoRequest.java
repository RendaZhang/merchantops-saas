package com.renda.merchantops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Echo request")
public class EchoRequest {

    @NotBlank(message = "message must not be blank")
    @Schema(description = "Any message to echo", example = "hello merchantops")
    private String message;

}
