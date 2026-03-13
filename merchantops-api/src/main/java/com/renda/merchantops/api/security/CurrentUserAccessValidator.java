package com.renda.merchantops.api.security;

import com.renda.merchantops.infra.persistence.entity.PermissionEntity;
import com.renda.merchantops.infra.persistence.entity.RoleEntity;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.repository.PermissionRepository;
import com.renda.merchantops.infra.repository.RoleRepository;
import com.renda.merchantops.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CurrentUserAccessValidator {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public ValidationResult validate(CurrentUser tokenUser) {
        Optional<UserEntity> user = userRepository.findByIdAndTenantId(tokenUser.getUserId(), tokenUser.getTenantId());
        if (user.isEmpty()) {
            return new ValidationResult(Status.USER_MISSING, null);
        }
        if (!"ACTIVE".equalsIgnoreCase(user.get().getStatus())) {
            return new ValidationResult(Status.USER_INACTIVE, null);
        }

        List<String> currentRoles = roleRepository.findRolesByUserIdAndTenantId(tokenUser.getUserId(), tokenUser.getTenantId())
                .stream()
                .map(RoleEntity::getRoleCode)
                .toList();
        List<String> currentPermissions = permissionRepository.findPermissionsByUserIdAndTenantId(tokenUser.getUserId(), tokenUser.getTenantId())
                .stream()
                .map(PermissionEntity::getPermissionCode)
                .toList();

        if (!sameCodeSet(tokenUser.getRoles(), currentRoles) || !sameCodeSet(tokenUser.getPermissions(), currentPermissions)) {
            return new ValidationResult(Status.CLAIMS_STALE, null);
        }

        CurrentUser validatedCurrentUser = new CurrentUser(
                user.get().getId(),
                user.get().getTenantId(),
                tokenUser.getTenantCode(),
                user.get().getUsername(),
                currentRoles,
                currentPermissions
        );
        return new ValidationResult(Status.USER_ACTIVE, validatedCurrentUser);
    }

    private boolean sameCodeSet(List<String> tokenCodes, List<String> currentCodes) {
        return normalizeCodes(tokenCodes).equals(normalizeCodes(currentCodes));
    }

    private Set<String> normalizeCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Set.of();
        }
        return codes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public enum Status {
        USER_ACTIVE,
        USER_INACTIVE,
        USER_MISSING,
        CLAIMS_STALE
    }

    public record ValidationResult(Status status, CurrentUser currentUser) {
    }
}
