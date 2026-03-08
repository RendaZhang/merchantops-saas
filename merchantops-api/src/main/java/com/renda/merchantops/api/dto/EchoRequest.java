package com.renda.merchantops.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EchoRequest {

    @NotBlank(message = "message must not be blank")
    private String message;

}
