package com.renda.merchantops.api.approval;

import com.renda.merchantops.domain.approval.ApprovalActionTypes;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Component
class ApprovalActionPermissionRegistry {

    private static final Map<String, ApprovalActionPermission> ACTION_PERMISSIONS = Map.of(
            ApprovalActionTypes.USER_STATUS_DISABLE, new ApprovalActionPermission("USER_READ", "USER_WRITE"),
            ApprovalActionTypes.IMPORT_JOB_SELECTIVE_REPLAY, new ApprovalActionPermission("USER_READ", "USER_WRITE"),
            ApprovalActionTypes.TICKET_COMMENT_CREATE, new ApprovalActionPermission("TICKET_READ", "TICKET_WRITE")
    );

    Set<String> readableActionTypes(Collection<String> grantedPermissions) {
        return actionTypesFor(grantedPermissions, ApprovalActionPermission::readPermission);
    }

    Set<String> reviewableActionTypes(Collection<String> grantedPermissions) {
        return actionTypesFor(grantedPermissions, ApprovalActionPermission::reviewPermission);
    }

    private Set<String> actionTypesFor(Collection<String> grantedPermissions,
                                       java.util.function.Function<ApprovalActionPermission, String> permissionExtractor) {
        Set<String> normalizedPermissions = normalizePermissions(grantedPermissions);
        Set<String> actionTypes = new LinkedHashSet<>();
        ACTION_PERMISSIONS.forEach((actionType, permission) -> {
            if (normalizedPermissions.contains(permissionExtractor.apply(permission))) {
                actionTypes.add(actionType);
            }
        });
        return Set.copyOf(actionTypes);
    }

    private Set<String> normalizePermissions(Collection<String> grantedPermissions) {
        if (grantedPermissions == null || grantedPermissions.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String permission : grantedPermissions) {
            String resolved = ApprovalActionTypes.normalize(permission);
            if (!resolved.isBlank()) {
                normalized.add(resolved);
            }
        }
        return normalized.isEmpty() ? Set.of() : Set.copyOf(normalized);
    }

    private record ApprovalActionPermission(String readPermission, String reviewPermission) {
    }
}
