package com.renda.merchantops.domain.user;

public interface UserCommandUseCase {

    void validateUsernameUnique(Long tenantId, String username);

    void validateUsernameUnique(Long tenantId, String username, Long excludeUserId);

    UserCreateResult createUser(Long tenantId, Long operatorId, String requestId, CreateUserCommand command);

    UserWriteResult updateUser(Long tenantId, Long operatorId, String requestId, Long userId, UpdateUserCommand command);

    UserWriteResult updateStatus(Long tenantId, Long operatorId, String requestId, Long userId, UpdateUserStatusCommand command);

    UserRoleAssignmentResult assignRoles(Long tenantId,
                                         Long operatorId,
                                         String requestId,
                                         Long userId,
                                         AssignUserRolesCommand command);

    void updatePassword(Long tenantId, Long userId, UpdateUserPasswordCommand command);
}
