package com.renda.merchantops.domain.user;

import java.time.LocalDateTime;
import java.util.List;

public record UserRoleAssignmentResult(
        Long id,
        Long tenantId,
        String username,
        List<String> roleCodes,
        List<String> permissionCodes,
        LocalDateTime updatedAt
) {
}
