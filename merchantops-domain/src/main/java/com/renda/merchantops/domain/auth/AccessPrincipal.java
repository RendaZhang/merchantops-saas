package com.renda.merchantops.domain.auth;

import java.util.List;

public record AccessPrincipal(
        Long userId,
        Long tenantId,
        String tenantCode,
        String username,
        List<String> roles,
        List<String> permissions
) {
}
