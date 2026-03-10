package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.user.command.UserCreateCommand;
import com.renda.merchantops.api.dto.user.command.UserPasswordUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserUpdateCommand;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserCommandService userCommandService;

    @Test
    void createUserShouldThrowBadRequestWhenUsernameAlreadyExists() {
        UserCreateCommand command = new UserCreateCommand("admin", "hash", "Admin", "admin@demo-shop.local", "ACTIVE");
        when(userRepository.existsByTenantIdAndUsername(1L, "admin")).thenReturn(true);

        assertBizException(
                () -> userCommandService.createUser(1L, command),
                ErrorCode.BAD_REQUEST,
                "username already exists in tenant"
        );

        verify(userRepository).existsByTenantIdAndUsername(1L, "admin");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void createUserShouldThrowBizErrorWhenFeatureIsNotReady() {
        UserCreateCommand command = new UserCreateCommand("new-user", "hash", "New User", "new-user@demo-shop.local", "ACTIVE");
        when(userRepository.existsByTenantIdAndUsername(1L, "new-user")).thenReturn(false);

        assertBizException(
                () -> userCommandService.createUser(1L, command),
                ErrorCode.BIZ_ERROR,
                "create user flow is not implemented yet"
        );
    }

    @Test
    void validateUsernameUniqueWithExcludeUserIdShouldRejectDuplicate() {
        when(userRepository.existsByTenantIdAndUsernameAndIdNot(1L, "admin", 3L)).thenReturn(true);

        assertBizException(
                () -> userCommandService.validateUsernameUnique(1L, "admin", 3L),
                ErrorCode.BAD_REQUEST,
                "username already exists in tenant"
        );
    }

    @Test
    void updateUserShouldThrowNotFoundWhenTenantUserMissing() {
        when(userRepository.findByIdAndTenantId(8L, 1L)).thenReturn(Optional.empty());

        assertBizException(
                () -> userCommandService.updateUser(1L, 8L, new UserUpdateCommand("Ops", "ops@demo.local", "ACTIVE")),
                ErrorCode.NOT_FOUND,
                "user not found"
        );
    }

    @Test
    void updateUserShouldThrowBizErrorWhenFeatureIsNotReady() {
        when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(new UserEntity()));

        assertBizException(
                () -> userCommandService.updateUser(1L, 1L, new UserUpdateCommand("Ops", "ops@demo.local", "ACTIVE")),
                ErrorCode.BIZ_ERROR,
                "update user flow is not implemented yet"
        );
    }

    @Test
    void updatePasswordShouldThrowBizErrorWhenFeatureIsNotReady() {
        when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(new UserEntity()));

        assertBizException(
                () -> userCommandService.updatePassword(1L, 1L, new UserPasswordUpdateCommand("new-hash")),
                ErrorCode.BIZ_ERROR,
                "update password flow is not implemented yet"
        );
    }

    private void assertBizException(ThrowingCall call, ErrorCode errorCode, String message) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException bizException = (BizException) ex;
                    assertThat(bizException.getErrorCode()).isEqualTo(errorCode);
                    assertThat(bizException.getMessage()).isEqualTo(message);
                });
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run();
    }
}
