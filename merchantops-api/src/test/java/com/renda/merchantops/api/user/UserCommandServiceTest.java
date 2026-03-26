package com.renda.merchantops.api.user;

import com.renda.merchantops.api.dto.user.command.UserCreateCommand;
import com.renda.merchantops.api.dto.user.command.UserCreateResponse;
import com.renda.merchantops.api.dto.user.command.UserPasswordUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserRoleAssignmentCommand;
import com.renda.merchantops.api.dto.user.command.UserRoleAssignmentResponse;
import com.renda.merchantops.api.dto.user.command.UserStatusUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserWriteResponse;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import com.renda.merchantops.domain.user.AssignUserRolesCommand;
import com.renda.merchantops.domain.user.CreateUserCommand;
import com.renda.merchantops.domain.user.UpdateUserPasswordCommand;
import com.renda.merchantops.domain.user.UpdateUserStatusCommand;
import com.renda.merchantops.domain.user.UpdateUserCommand;
import com.renda.merchantops.domain.user.UserCommandUseCase;
import com.renda.merchantops.domain.user.UserCreateResult;
import com.renda.merchantops.domain.user.UserRoleAssignmentResult;
import com.renda.merchantops.domain.user.UserWriteResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    @Mock
    private UserCommandUseCase userCommandUseCase;

    @InjectMocks
    private UserCommandService userCommandService;

    @Test
    void validateUsernameUniqueShouldDelegateIncludingExcludeId() {
        userCommandService.validateUsernameUnique(1L, "admin");
        userCommandService.validateUsernameUnique(1L, "admin", 3L);

        verify(userCommandUseCase).validateUsernameUnique(1L, "admin");
        verify(userCommandUseCase).validateUsernameUnique(1L, "admin", 3L);
    }

    @Test
    void createUserShouldNormalizeRequestIdMapCommandAndMapResult() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 10, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 10, 10, 30);
        when(userCommandUseCase.createUser(eq(1L), eq(101L), eq("req-1"), any(CreateUserCommand.class)))
                .thenReturn(new UserCreateResult(
                        205L,
                        1L,
                        "cashier",
                        "Cashier User",
                        "cashier@demo-shop.local",
                        "ACTIVE",
                        List.of("READ_ONLY", "OPS_USER"),
                        createdAt,
                        updatedAt
                ));

        UserCreateResponse response = userCommandService.createUser(
                1L,
                101L,
                " req-1 ",
                new UserCreateCommand("cashier", "123456", "Cashier User", "cashier@demo-shop.local", List.of("READ_ONLY", "OPS_USER"))
        );

        ArgumentCaptor<CreateUserCommand> commandCaptor = ArgumentCaptor.forClass(CreateUserCommand.class);
        verify(userCommandUseCase).createUser(eq(1L), eq(101L), eq("req-1"), commandCaptor.capture());
        assertThat(commandCaptor.getValue().username()).isEqualTo("cashier");
        assertThat(commandCaptor.getValue().password()).isEqualTo("123456");
        assertThat(commandCaptor.getValue().displayName()).isEqualTo("Cashier User");
        assertThat(commandCaptor.getValue().email()).isEqualTo("cashier@demo-shop.local");
        assertThat(commandCaptor.getValue().roleCodes()).containsExactly("READ_ONLY", "OPS_USER");
        assertThat(response.getId()).isEqualTo(205L);
        assertThat(response.getTenantId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("cashier");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getRoleCodes()).containsExactly("READ_ONLY", "OPS_USER");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void createUserShouldRejectMissingRequestIdBeforeCallingDomain() {
        assertThatThrownBy(() -> userCommandService.createUser(
                1L,
                101L,
                "   ",
                new UserCreateCommand("cashier", "123456", "Cashier User", "cashier@demo-shop.local", List.of("READ_ONLY"))
        )).isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void updateUserShouldNormalizeRequestIdMapCommandAndMapResult() {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 10, 11, 0);
        when(userCommandUseCase.updateUser(eq(1L), eq(101L), eq("req-2"), eq(8L), any(UpdateUserCommand.class)))
                .thenReturn(new UserWriteResult(
                        8L,
                        1L,
                        "ops",
                        "Updated Ops",
                        "ops@demo.local",
                        "ACTIVE",
                        updatedAt
                ));

        UserWriteResponse response = userCommandService.updateUser(
                1L,
                101L,
                " req-2 ",
                8L,
                new UserUpdateCommand("Updated Ops", "ops@demo.local")
        );

        ArgumentCaptor<UpdateUserCommand> commandCaptor = ArgumentCaptor.forClass(UpdateUserCommand.class);
        verify(userCommandUseCase).updateUser(eq(1L), eq(101L), eq("req-2"), eq(8L), commandCaptor.capture());
        assertThat(commandCaptor.getValue().displayName()).isEqualTo("Updated Ops");
        assertThat(commandCaptor.getValue().email()).isEqualTo("ops@demo.local");
        assertThat(response.getUsername()).isEqualTo("ops");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void updateStatusShouldNormalizeRequestIdAndMapCommand() {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 10, 12, 0);
        when(userCommandUseCase.updateStatus(eq(1L), eq(101L), eq("req-3"), eq(8L), any(UpdateUserStatusCommand.class)))
                .thenReturn(new UserWriteResult(
                        8L,
                        1L,
                        "ops",
                        "Ops User",
                        "ops@demo.local",
                        "DISABLED",
                        updatedAt
                ));

        UserWriteResponse response = userCommandService.updateStatus(
                1L,
                101L,
                " req-3 ",
                8L,
                new UserStatusUpdateCommand("DISABLED")
        );

        ArgumentCaptor<UpdateUserStatusCommand> commandCaptor = ArgumentCaptor.forClass(UpdateUserStatusCommand.class);
        verify(userCommandUseCase).updateStatus(eq(1L), eq(101L), eq("req-3"), eq(8L), commandCaptor.capture());
        assertThat(commandCaptor.getValue().status()).isEqualTo("DISABLED");
        assertThat(response.getStatus()).isEqualTo("DISABLED");
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void assignRolesShouldNormalizeRequestIdMapCommandAndMapResult() {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 10, 12, 30);
        when(userCommandUseCase.assignRoles(eq(1L), eq(101L), eq("req-4"), eq(8L), any(AssignUserRolesCommand.class)))
                .thenReturn(new UserRoleAssignmentResult(
                        8L,
                        1L,
                        "ops",
                        List.of("TENANT_ADMIN"),
                        List.of("USER_READ", "USER_WRITE"),
                        updatedAt
                ));

        UserRoleAssignmentResponse response = userCommandService.assignRoles(
                1L,
                101L,
                " req-4 ",
                8L,
                new UserRoleAssignmentCommand(List.of("TENANT_ADMIN"))
        );

        ArgumentCaptor<AssignUserRolesCommand> commandCaptor = ArgumentCaptor.forClass(AssignUserRolesCommand.class);
        verify(userCommandUseCase).assignRoles(eq(1L), eq(101L), eq("req-4"), eq(8L), commandCaptor.capture());
        assertThat(commandCaptor.getValue().roleCodes()).containsExactly("TENANT_ADMIN");
        assertThat(response.getRoleCodes()).containsExactly("TENANT_ADMIN");
        assertThat(response.getPermissionCodes()).containsExactly("USER_READ", "USER_WRITE");
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void updatePasswordShouldMapCommand() {
        userCommandService.updatePassword(1L, 8L, new UserPasswordUpdateCommand("new-hash"));

        ArgumentCaptor<UpdateUserPasswordCommand> commandCaptor = ArgumentCaptor.forClass(UpdateUserPasswordCommand.class);
        verify(userCommandUseCase).updatePassword(eq(1L), eq(8L), commandCaptor.capture());
        assertThat(commandCaptor.getValue().passwordHash()).isEqualTo("new-hash");
    }
}
