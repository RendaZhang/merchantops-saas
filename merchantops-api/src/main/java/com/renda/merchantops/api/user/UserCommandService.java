package com.renda.merchantops.api.user;

import com.renda.merchantops.api.context.RequestIdPolicy;
import com.renda.merchantops.api.dto.user.command.UserCreateCommand;
import com.renda.merchantops.api.dto.user.command.UserCreateResponse;
import com.renda.merchantops.api.dto.user.command.UserPasswordUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserRoleAssignmentCommand;
import com.renda.merchantops.api.dto.user.command.UserRoleAssignmentResponse;
import com.renda.merchantops.api.dto.user.command.UserStatusUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserWriteResponse;
import com.renda.merchantops.domain.user.AssignUserRolesCommand;
import com.renda.merchantops.domain.user.UpdateUserPasswordCommand;
import com.renda.merchantops.domain.user.UpdateUserStatusCommand;
import com.renda.merchantops.domain.user.UserCommandUseCase;
import com.renda.merchantops.domain.user.UserCreateResult;
import com.renda.merchantops.domain.user.UserRoleAssignmentResult;
import com.renda.merchantops.domain.user.UserWriteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCommandService {

    private final UserCommandUseCase userCommandUseCase;

    public void validateUsernameUnique(Long tenantId, String username) {
        userCommandUseCase.validateUsernameUnique(tenantId, username);
    }

    public void validateUsernameUnique(Long tenantId, String username, Long excludeUserId) {
        userCommandUseCase.validateUsernameUnique(tenantId, username, excludeUserId);
    }

    @Transactional
    public UserCreateResponse createUser(Long tenantId, Long operatorId, String requestId, UserCreateCommand command) {
        UserCreateResult result = userCommandUseCase.createUser(
                tenantId,
                operatorId,
                RequestIdPolicy.requireNormalized(requestId),
                new com.renda.merchantops.domain.user.CreateUserCommand(
                        command == null ? null : command.getUsername(),
                        command == null ? null : command.getPassword(),
                        command == null ? null : command.getDisplayName(),
                        command == null ? null : command.getEmail(),
                        command == null ? null : command.getRoleCodes()
                )
        );
        return new UserCreateResponse(
                result.id(),
                result.tenantId(),
                result.username(),
                result.displayName(),
                result.email(),
                result.status(),
                result.roleCodes(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    @Transactional
    public UserWriteResponse updateUser(Long tenantId, Long operatorId, String requestId, Long userId, UserUpdateCommand command) {
        return toUserWriteResponse(userCommandUseCase.updateUser(
                tenantId,
                operatorId,
                RequestIdPolicy.requireNormalized(requestId),
                userId,
                new com.renda.merchantops.domain.user.UpdateUserCommand(
                        command == null ? null : command.getDisplayName(),
                        command == null ? null : command.getEmail()
                )
        ));
    }

    @Transactional
    public UserWriteResponse updateStatus(Long tenantId, Long operatorId, String requestId, Long userId, UserStatusUpdateCommand command) {
        return toUserWriteResponse(userCommandUseCase.updateStatus(
                tenantId,
                operatorId,
                RequestIdPolicy.requireNormalized(requestId),
                userId,
                new UpdateUserStatusCommand(command == null ? null : command.getStatus())
        ));
    }

    @Transactional
    public UserRoleAssignmentResponse assignRoles(Long tenantId, Long operatorId, String requestId, Long userId, UserRoleAssignmentCommand command) {
        UserRoleAssignmentResult result = userCommandUseCase.assignRoles(
                tenantId,
                operatorId,
                RequestIdPolicy.requireNormalized(requestId),
                userId,
                new AssignUserRolesCommand(command == null ? null : command.getRoleCodes())
        );
        return new UserRoleAssignmentResponse(
                result.id(),
                result.tenantId(),
                result.username(),
                result.roleCodes(),
                result.permissionCodes(),
                result.updatedAt()
        );
    }

    public void updatePassword(Long tenantId, Long userId, UserPasswordUpdateCommand command) {
        userCommandUseCase.updatePassword(
                tenantId,
                userId,
                new UpdateUserPasswordCommand(command == null ? null : command.getPasswordHash())
        );
    }

    private UserWriteResponse toUserWriteResponse(UserWriteResult result) {
        return new UserWriteResponse(
                result.id(),
                result.tenantId(),
                result.username(),
                result.displayName(),
                result.email(),
                result.status(),
                result.updatedAt()
        );
    }
}
