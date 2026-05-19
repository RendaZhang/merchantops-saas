package com.renda.merchantops.domain.user;

import com.renda.merchantops.domain.auth.PasswordHasher;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    @Mock
    private UserQueryPort userQueryPort;

    @Mock
    private RoleCatalogPort roleCatalogPort;

    @Mock
    private UserCommandPort userCommandPort;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private UserAuditPort userAuditPort;

    @Test
    void createUserShouldPersistAndAuditNormalizedRoleCodes() {
        UserCommandService service = new UserCommandService(userQueryPort, roleCatalogPort, userCommandPort, passwordHasher, userAuditPort);
        when(userQueryPort.usernameExists(1L, "cashier")).thenReturn(false);
        when(roleCatalogPort.findRolesByCodes(1L, List.of("READ_ONLY", "OPS_USER")))
                .thenReturn(List.of(
                        new RoleItem(13L, "READ_ONLY", "Read Only"),
                        new RoleItem(12L, "OPS_USER", "Ops User")
                ));
        when(passwordHasher.encode("123456")).thenReturn("bcrypt-hash");
        when(userCommandPort.createUser(org.mockito.ArgumentMatchers.any(NewUserDraft.class)))
                .thenAnswer(invocation -> {
                    NewUserDraft draft = invocation.getArgument(0);
                    return new ManagedUser(
                            205L,
                            draft.tenantId(),
                            draft.username(),
                            draft.passwordHash(),
                            draft.displayName(),
                            draft.email(),
                            draft.status(),
                            draft.createdAt(),
                            draft.updatedAt(),
                            draft.createdBy(),
                            draft.updatedBy()
                    );
                });

        UserCreateResult result = service.createUser(
                1L,
                101L,
                "req-1",
                new CreateUserCommand("cashier", "123456", "Cashier User", "cashier@demo.local", List.of("READ_ONLY", "OPS_USER", "READ_ONLY"))
        );

        assertThat(result.id()).isEqualTo(205L);
        assertThat(result.roleCodes()).containsExactly("READ_ONLY", "OPS_USER");

        ArgumentCaptor<NewUserDraft> draftCaptor = ArgumentCaptor.forClass(NewUserDraft.class);
        verify(userCommandPort).createUser(draftCaptor.capture());
        assertThat(draftCaptor.getValue().passwordHash()).isEqualTo("bcrypt-hash");
        verify(userCommandPort).replaceUserRoles(1L, 205L, List.of(13L, 12L));
        verify(userAuditPort).recordEvent(eq(1L), eq("USER"), eq(205L), eq("USER_CREATED"), eq(101L), eq("req-1"),
                eq(null), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateStatusShouldRejectInvalidStatus() {
        UserCommandService service = new UserCommandService(userQueryPort, roleCatalogPort, userCommandPort, passwordHasher, userAuditPort);
        when(userCommandPort.findManagedUser(1L, 1L))
                .thenReturn(Optional.of(new ManagedUser(
                        1L, 1L, "cashier", "hash", "Cashier", "cashier@demo.local", "ACTIVE",
                        LocalDateTime.now().minusDays(1), LocalDateTime.now().minusHours(1), 88L, 89L
                )));

        assertThatThrownBy(() -> service.updateStatus(1L, 101L, "req-1", 1L, new UpdateUserStatusCommand("ARCHIVED")))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }
}
