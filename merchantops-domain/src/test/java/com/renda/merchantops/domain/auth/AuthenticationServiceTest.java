package com.renda.merchantops.domain.auth;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthAccessPort authAccessPort;

    @Mock
    private PasswordHasher passwordHasher;

    @Test
    void authenticateShouldReturnCurrentTenantRolesAndPermissions() {
        AuthenticationService service = new AuthenticationService(authAccessPort, passwordHasher);
        when(authAccessPort.findTenantByCode("demo-shop"))
                .thenReturn(Optional.of(new TenantAccount(1L, "demo-shop", "ACTIVE")));
        when(authAccessPort.findUserByTenantIdAndUsername(1L, "admin"))
                .thenReturn(Optional.of(new AccessUserAccount(101L, 1L, "admin", "bcrypt-hash", "ACTIVE")));
        when(passwordHasher.matches("123456", "bcrypt-hash")).thenReturn(true);
        when(authAccessPort.findRoleCodes(101L, 1L)).thenReturn(List.of("TENANT_ADMIN"));
        when(authAccessPort.findPermissionCodes(101L, 1L)).thenReturn(List.of("USER_READ", "USER_WRITE"));

        AuthenticationResult result = service.authenticate(new AuthenticationCommand("demo-shop", "admin", "123456"));

        assertThat(result.userId()).isEqualTo(101L);
        assertThat(result.tenantId()).isEqualTo(1L);
        assertThat(result.tenantCode()).isEqualTo("demo-shop");
        assertThat(result.username()).isEqualTo("admin");
        assertThat(result.roles()).containsExactly("TENANT_ADMIN");
        assertThat(result.permissions()).containsExactly("USER_READ", "USER_WRITE");
    }

    @Test
    void authenticateShouldRejectWrongPassword() {
        AuthenticationService service = new AuthenticationService(authAccessPort, passwordHasher);
        when(authAccessPort.findTenantByCode("demo-shop"))
                .thenReturn(Optional.of(new TenantAccount(1L, "demo-shop", "ACTIVE")));
        when(authAccessPort.findUserByTenantIdAndUsername(1L, "admin"))
                .thenReturn(Optional.of(new AccessUserAccount(101L, 1L, "admin", "bcrypt-hash", "ACTIVE")));
        when(passwordHasher.matches("wrong", "bcrypt-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.authenticate(new AuthenticationCommand("demo-shop", "admin", "wrong")))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }
}
