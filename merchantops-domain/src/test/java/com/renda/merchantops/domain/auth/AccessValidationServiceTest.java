package com.renda.merchantops.domain.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessValidationServiceTest {

    @Mock
    private AuthAccessPort authAccessPort;

    @Test
    void validateShouldDetectStaleClaims() {
        AccessValidationService service = new AccessValidationService(authAccessPort);
        when(authAccessPort.findTenantById(1L))
                .thenReturn(Optional.of(new TenantAccount(1L, "demo-shop", "ACTIVE")));
        when(authAccessPort.findUserByIdAndTenantId(101L, 1L))
                .thenReturn(Optional.of(new AccessUserAccount(101L, 1L, "admin", "bcrypt-hash", "ACTIVE")));
        when(authAccessPort.findRoleCodes(101L, 1L)).thenReturn(List.of("TENANT_ADMIN"));
        when(authAccessPort.findPermissionCodes(101L, 1L)).thenReturn(List.of("USER_READ", "USER_WRITE"));

        AccessValidationResult result = service.validate(new AccessPrincipal(
                101L,
                1L,
                "demo-shop",
                "admin",
                List.of("READ_ONLY"),
                List.of("USER_READ")
        ));

        assertThat(result.status()).isEqualTo(AccessValidationStatus.CLAIMS_STALE);
        assertThat(result.currentUser()).isNull();
    }

    @Test
    void validateShouldReturnUpdatedPrincipalWhenClaimsMatch() {
        AccessValidationService service = new AccessValidationService(authAccessPort);
        when(authAccessPort.findTenantById(1L))
                .thenReturn(Optional.of(new TenantAccount(1L, "demo-shop", "ACTIVE")));
        when(authAccessPort.findUserByIdAndTenantId(101L, 1L))
                .thenReturn(Optional.of(new AccessUserAccount(101L, 1L, "admin", "bcrypt-hash", "ACTIVE")));
        when(authAccessPort.findRoleCodes(101L, 1L)).thenReturn(List.of("TENANT_ADMIN"));
        when(authAccessPort.findPermissionCodes(101L, 1L)).thenReturn(List.of("USER_READ", "USER_WRITE"));

        AccessValidationResult result = service.validate(new AccessPrincipal(
                101L,
                1L,
                "demo-shop",
                "stale-name",
                List.of("TENANT_ADMIN"),
                List.of("USER_READ", "USER_WRITE")
        ));

        assertThat(result.status()).isEqualTo(AccessValidationStatus.USER_ACTIVE);
        assertThat(result.currentUser().username()).isEqualTo("admin");
    }

    @Test
    void validateShouldRejectInactiveTenant() {
        AccessValidationService service = new AccessValidationService(authAccessPort);
        when(authAccessPort.findTenantById(1L))
                .thenReturn(Optional.of(new TenantAccount(1L, "demo-shop", "DISABLED")));

        AccessValidationResult result = service.validate(new AccessPrincipal(
                101L,
                1L,
                "demo-shop",
                "admin",
                List.of("TENANT_ADMIN"),
                List.of("USER_READ", "USER_WRITE")
        ));

        assertThat(result.status()).isEqualTo(AccessValidationStatus.TENANT_INACTIVE);
        assertThat(result.currentUser()).isNull();
    }
}
