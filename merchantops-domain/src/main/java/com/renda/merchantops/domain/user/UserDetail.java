package com.renda.merchantops.domain.user;

import java.time.LocalDateTime;
import java.util.List;

public record UserDetail(
        Long id,
        Long tenantId,
        String username,
        String displayName,
        String email,
        String status,
        List<String> roleCodes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
