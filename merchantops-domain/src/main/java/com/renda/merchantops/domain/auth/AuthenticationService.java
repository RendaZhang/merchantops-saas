package com.renda.merchantops.domain.auth;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;

import java.util.List;

public final class AuthenticationService implements AuthenticationUseCase {

    private final AuthAccessPort authAccessPort;
    private final PasswordHasher passwordHasher;

    public AuthenticationService(AuthAccessPort authAccessPort, PasswordHasher passwordHasher) {
        this.authAccessPort = authAccessPort;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public AuthenticationResult authenticate(AuthenticationCommand command) {
        PasswordPolicy.requireNoBoundaryWhitespace(command.password());

        TenantAccount tenant = authAccessPort.findTenantByCode(command.tenantCode())
                .orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, "tenant not found"));
        if (!"ACTIVE".equalsIgnoreCase(tenant.status())) {
            throw new BizException(ErrorCode.FORBIDDEN, "tenant is not active");
        }

        AccessUserAccount user = authAccessPort.findUserByTenantIdAndUsername(tenant.id(), command.username())
                .orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, "username or password is incorrect"));
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new BizException(ErrorCode.FORBIDDEN, "user is not active");
        }
        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "username or password is incorrect");
        }

        List<String> roles = authAccessPort.findRoleCodes(user.id(), tenant.id());
        List<String> permissions = authAccessPort.findPermissionCodes(user.id(), tenant.id());
        return new AuthenticationResult(
                user.id(),
                tenant.id(),
                tenant.tenantCode(),
                user.username(),
                roles,
                permissions
        );
    }
}
