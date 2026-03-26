package com.renda.merchantops.domain.auth;

public record TenantAccount(
        Long id,
        String tenantCode,
        String status
) {
}
