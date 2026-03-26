package com.renda.merchantops.domain.auth;

public record AuthenticationCommand(
        String tenantCode,
        String username,
        String password
) {
}
