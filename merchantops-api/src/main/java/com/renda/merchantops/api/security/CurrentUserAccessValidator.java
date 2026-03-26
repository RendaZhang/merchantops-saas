package com.renda.merchantops.api.security;

import com.renda.merchantops.domain.auth.AccessPrincipal;
import com.renda.merchantops.domain.auth.AccessValidationResult;
import com.renda.merchantops.domain.auth.AccessValidationStatus;
import com.renda.merchantops.domain.auth.AccessValidationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserAccessValidator {

    private final AccessValidationUseCase accessValidationUseCase;

    public ValidationResult validate(CurrentUser tokenUser) {
        AccessValidationResult result = accessValidationUseCase.validate(new AccessPrincipal(
                tokenUser.getUserId(),
                tokenUser.getTenantId(),
                tokenUser.getTenantCode(),
                tokenUser.getUsername(),
                tokenUser.getRoles(),
                tokenUser.getPermissions()
        ));

        if (result.currentUser() == null) {
            return new ValidationResult(toStatus(result.status()), null);
        }

        return new ValidationResult(
                toStatus(result.status()),
                new CurrentUser(
                        result.currentUser().userId(),
                        result.currentUser().tenantId(),
                        result.currentUser().tenantCode(),
                        result.currentUser().username(),
                        result.currentUser().roles(),
                        result.currentUser().permissions()
                )
        );
    }

    private Status toStatus(AccessValidationStatus status) {
        return switch (status) {
            case USER_ACTIVE -> Status.USER_ACTIVE;
            case USER_INACTIVE -> Status.USER_INACTIVE;
            case USER_MISSING -> Status.USER_MISSING;
            case CLAIMS_STALE -> Status.CLAIMS_STALE;
        };
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
