package com.renda.merchantops.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;

    private String tokenType;

    private long expiresIn;

}
