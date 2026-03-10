package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.user.command.UserCreateCommand;
import com.renda.merchantops.api.dto.user.command.UserPasswordUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserUpdateCommand;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Write-side user service.
 * Immutable after creation: id, tenantId, username, createdAt.
 * Updatable by command: displayName, email, status, passwordHash.
 * System-managed only: updatedAt.
 */
@Service
@RequiredArgsConstructor
public class UserCommandService {

    private final UserRepository userRepository;

    public void validateUsernameUnique(Long tenantId, String username) {
        if (userRepository.existsByTenantIdAndUsername(tenantId, username)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "username already exists in tenant");
        }
    }

    public void validateUsernameUnique(Long tenantId, String username, Long excludeUserId) {
        if (excludeUserId == null) {
            validateUsernameUnique(tenantId, username);
            return;
        }
        if (userRepository.existsByTenantIdAndUsernameAndIdNot(tenantId, username, excludeUserId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "username already exists in tenant");
        }
    }

    public void createUser(Long tenantId, UserCreateCommand command) {
        validateUsernameUnique(tenantId, command.getUsername());
        throwFeatureNotReady("create user flow");
    }

    public void updateUser(Long tenantId, Long userId, UserUpdateCommand command) {
        requireTenantUser(tenantId, userId);
        throwFeatureNotReady("update user flow");
    }

    public void updatePassword(Long tenantId, Long userId, UserPasswordUpdateCommand command) {
        requireTenantUser(tenantId, userId);
        throwFeatureNotReady("update password flow");
    }

    private UserEntity requireTenantUser(Long tenantId, Long userId) {
        return userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "user not found"));
    }

    private void throwFeatureNotReady(String action) {
        throw new BizException(ErrorCode.BIZ_ERROR, action + " is not implemented yet");
    }
}
