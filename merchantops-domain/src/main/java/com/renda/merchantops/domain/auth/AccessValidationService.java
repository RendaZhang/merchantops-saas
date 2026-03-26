package com.renda.merchantops.domain.auth;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AccessValidationService implements AccessValidationUseCase {

    private final AuthAccessPort authAccessPort;

    public AccessValidationService(AuthAccessPort authAccessPort) {
        this.authAccessPort = authAccessPort;
    }

    @Override
    public AccessValidationResult validate(AccessPrincipal tokenUser) {
        AccessUserAccount user = authAccessPort.findUserByIdAndTenantId(tokenUser.userId(), tokenUser.tenantId()).orElse(null);
        if (user == null) {
            return new AccessValidationResult(AccessValidationStatus.USER_MISSING, null);
        }
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            return new AccessValidationResult(AccessValidationStatus.USER_INACTIVE, null);
        }

        List<String> currentRoles = authAccessPort.findRoleCodes(tokenUser.userId(), tokenUser.tenantId());
        List<String> currentPermissions = authAccessPort.findPermissionCodes(tokenUser.userId(), tokenUser.tenantId());
        if (!sameCodeSet(tokenUser.roles(), currentRoles) || !sameCodeSet(tokenUser.permissions(), currentPermissions)) {
            return new AccessValidationResult(AccessValidationStatus.CLAIMS_STALE, null);
        }

        return new AccessValidationResult(
                AccessValidationStatus.USER_ACTIVE,
                new ValidatedAccessPrincipal(
                        user.id(),
                        user.tenantId(),
                        tokenUser.tenantCode(),
                        user.username(),
                        currentRoles,
                        currentPermissions
                )
        );
    }

    private boolean sameCodeSet(List<String> tokenCodes, List<String> currentCodes) {
        return normalizeCodes(tokenCodes).equals(normalizeCodes(currentCodes));
    }

    private Set<String> normalizeCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String code : codes) {
            if (code != null) {
                String trimmed = code.trim();
                if (!trimmed.isEmpty()) {
                    normalized.add(trimmed);
                }
            }
        }
        return normalized;
    }
}
