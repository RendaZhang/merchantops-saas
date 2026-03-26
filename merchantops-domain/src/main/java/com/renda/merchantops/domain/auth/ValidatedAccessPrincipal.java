package com.renda.merchantops.domain.auth;

import java.util.List;

public record ValidatedAccessPrincipal(
        Long userId,
        Long tenantId,
        String tenantCode,
        String username,
        List<String> roles,
        List<String> permissions
) {
}
