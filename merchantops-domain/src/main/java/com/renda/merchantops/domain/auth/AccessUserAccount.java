package com.renda.merchantops.domain.auth;

public record AccessUserAccount(
        Long id,
        Long tenantId,
        String username,
        String passwordHash,
        String status
) {
}
