package com.renda.merchantops.api.dto.dev;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Echo response")
public class EchoResponse {

    @Schema(description = "Echoed message from request body", example = "hello merchantops")
    private String message;
}
