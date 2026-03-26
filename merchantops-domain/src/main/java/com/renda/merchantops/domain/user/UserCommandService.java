package com.renda.merchantops.domain.user;

import com.renda.merchantops.domain.auth.PasswordHasher;
import com.renda.merchantops.domain.auth.PasswordPolicy;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class UserCommandService implements UserCommandUseCase {

    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "DISABLED");

    private final UserQueryPort userQueryPort;
    private final RoleCatalogPort roleCatalogPort;
    private final UserCommandPort userCommandPort;
    private final PasswordHasher passwordHasher;
    private final UserAuditPort userAuditPort;

    public UserCommandService(UserQueryPort userQueryPort,
                              RoleCatalogPort roleCatalogPort,
                              UserCommandPort userCommandPort,
                              PasswordHasher passwordHasher,
                              UserAuditPort userAuditPort) {
        this.userQueryPort = userQueryPort;
        this.roleCatalogPort = roleCatalogPort;
        this.userCommandPort = userCommandPort;
        this.passwordHasher = passwordHasher;
        this.userAuditPort = userAuditPort;
    }

    @Override
    public void validateUsernameUnique(Long tenantId, String username) {
        if (userQueryPort.usernameExists(tenantId, username)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "username already exists in tenant");
        }
    }

    @Override
    public void validateUsernameUnique(Long tenantId, String username, Long excludeUserId) {
        if (excludeUserId == null) {
            validateUsernameUnique(tenantId, username);
            return;
        }
        if (userQueryPort.usernameExists(tenantId, username, excludeUserId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "username already exists in tenant");
        }
    }

    @Override
    public UserCreateResult createUser(Long tenantId, Long operatorId, String requestId, CreateUserCommand command) {
        Long resolvedOperatorId = requireOperatorId(operatorId);
        String resolvedRequestId = requireRequestId(requestId);
        String username = normalizeRequiredText(command == null ? null : command.username(), "username");
        String displayName = normalizeRequiredText(command == null ? null : command.displayName(), "displayName");
        String password = requireNonBlankPreserveValue(command == null ? null : command.password(), "password");
        PasswordPolicy.requireNoBoundaryWhitespace(password);
        String email = normalizeOptionalText(command == null ? null : command.email());
        List<String> roleCodes = normalizeRoleCodes(command == null ? null : command.roleCodes());

        validateUsernameUnique(tenantId, username);

        List<RoleItem> roles = roleCatalogPort.findRolesByCodes(tenantId, roleCodes);
        if (roles.size() != roleCodes.size()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "roleCodes must exist in current tenant");
        }

        LocalDateTime now = LocalDateTime.now();
        ManagedUser savedUser = userCommandPort.createUser(new NewUserDraft(
                tenantId,
                username,
                passwordHasher.encode(password),
                displayName,
                email,
                "ACTIVE",
                now,
                now,
                resolvedOperatorId,
                resolvedOperatorId
        ));
        userCommandPort.replaceUserRoles(savedUser.id(), roles.stream().map(RoleItem::id).toList());

        userAuditPort.recordEvent(
                tenantId,
                "USER",
                savedUser.id(),
                "USER_CREATED",
                resolvedOperatorId,
                resolvedRequestId,
                null,
                snapshotUser(savedUser, roleCodes)
        );

        return new UserCreateResult(
                savedUser.id(),
                savedUser.tenantId(),
                savedUser.username(),
                savedUser.displayName(),
                savedUser.email(),
                savedUser.status(),
                roleCodes,
                savedUser.createdAt(),
                savedUser.updatedAt()
        );
    }

    @Override
    public UserWriteResult updateUser(Long tenantId, Long operatorId, String requestId, Long userId, UpdateUserCommand command) {
        ManagedUser user = requireTenantUser(tenantId, userId);
        Long resolvedOperatorId = requireOperatorId(operatorId);
        String resolvedRequestId = requireRequestId(requestId);
        Map<String, Object> before = snapshotUser(user, roleCatalogPort.findRoleCodesByUserId(tenantId, user.id()));

        ManagedUser savedUser = userCommandPort.saveManagedUser(new ManagedUser(
                user.id(),
                user.tenantId(),
                user.username(),
                user.passwordHash(),
                normalizeRequiredText(command == null ? null : command.displayName(), "displayName"),
                normalizeOptionalText(command == null ? null : command.email()),
                user.status(),
                user.createdAt(),
                LocalDateTime.now(),
                user.createdBy(),
                resolvedOperatorId
        ));

        userAuditPort.recordEvent(
                tenantId,
                "USER",
                savedUser.id(),
                "USER_UPDATED",
                resolvedOperatorId,
                resolvedRequestId,
                before,
                snapshotUser(savedUser, roleCatalogPort.findRoleCodesByUserId(tenantId, savedUser.id()))
        );

        return toUserWriteResult(savedUser);
    }

    @Override
    public UserWriteResult updateStatus(Long tenantId, Long operatorId, String requestId, Long userId, UpdateUserStatusCommand command) {
        ManagedUser user = requireTenantUser(tenantId, userId);
        Long resolvedOperatorId = requireOperatorId(operatorId);
        String resolvedRequestId = requireRequestId(requestId);
        Map<String, Object> before = snapshotUser(user, roleCatalogPort.findRoleCodesByUserId(tenantId, user.id()));

        ManagedUser savedUser = userCommandPort.saveManagedUser(new ManagedUser(
                user.id(),
                user.tenantId(),
                user.username(),
                user.passwordHash(),
                user.displayName(),
                user.email(),
                normalizeAllowedStatus(command == null ? null : command.status()),
                user.createdAt(),
                LocalDateTime.now(),
                user.createdBy(),
                resolvedOperatorId
        ));

        userAuditPort.recordEvent(
                tenantId,
                "USER",
                savedUser.id(),
                "USER_STATUS_UPDATED",
                resolvedOperatorId,
                resolvedRequestId,
                before,
                snapshotUser(savedUser, roleCatalogPort.findRoleCodesByUserId(tenantId, savedUser.id()))
        );

        return toUserWriteResult(savedUser);
    }

    @Override
    public UserRoleAssignmentResult assignRoles(Long tenantId,
                                                Long operatorId,
                                                String requestId,
                                                Long userId,
                                                AssignUserRolesCommand command) {
        ManagedUser user = requireTenantUser(tenantId, userId);
        Long resolvedOperatorId = requireOperatorId(operatorId);
        String resolvedRequestId = requireRequestId(requestId);
        Map<String, Object> before = snapshotUser(user, roleCatalogPort.findRoleCodesByUserId(tenantId, user.id()));
        List<String> roleCodes = normalizeRoleCodes(command == null ? null : command.roleCodes());
        List<RoleItem> roles = roleCatalogPort.findRolesByCodes(tenantId, roleCodes);
        if (roles.size() != roleCodes.size()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "roleCodes must exist in current tenant");
        }

        userCommandPort.replaceUserRoles(user.id(), roles.stream().map(RoleItem::id).toList());
        ManagedUser savedUser = userCommandPort.saveManagedUser(new ManagedUser(
                user.id(),
                user.tenantId(),
                user.username(),
                user.passwordHash(),
                user.displayName(),
                user.email(),
                user.status(),
                user.createdAt(),
                LocalDateTime.now(),
                user.createdBy(),
                resolvedOperatorId
        ));

        List<String> assignedRoleCodes = roles.stream().map(RoleItem::roleCode).toList();
        List<String> permissionCodes = roleCatalogPort.findPermissionCodesByUserId(savedUser.tenantId(), savedUser.id());

        userAuditPort.recordEvent(
                tenantId,
                "USER",
                savedUser.id(),
                "USER_ROLES_UPDATED",
                resolvedOperatorId,
                resolvedRequestId,
                before,
                snapshotUser(savedUser, assignedRoleCodes)
        );

        return new UserRoleAssignmentResult(
                savedUser.id(),
                savedUser.tenantId(),
                savedUser.username(),
                assignedRoleCodes,
                permissionCodes,
                savedUser.updatedAt()
        );
    }

    @Override
    public void updatePassword(Long tenantId, Long userId, UpdateUserPasswordCommand command) {
        requireTenantUser(tenantId, userId);
        throwFeatureNotReady("update password flow");
    }

    private ManagedUser requireTenantUser(Long tenantId, Long userId) {
        return userCommandPort.findManagedUser(tenantId, userId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "user not found"));
    }

    private Long requireOperatorId(Long operatorId) {
        if (operatorId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "user context missing");
        }
        return operatorId;
    }

    private String requireRequestId(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "request id missing");
        }
        return requestId.trim();
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " must not be blank");
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String requireNonBlankPreserveValue(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, fieldName + " must not be blank");
        }
        return value;
    }

    private String normalizeAllowedStatus(String value) {
        String status = normalizeRequiredText(value, "status");
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "status must be one of ACTIVE, DISABLED");
        }
        return status;
    }

    private List<String> normalizeRoleCodes(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "roleCodes must not be empty");
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String roleCode : roleCodes) {
            if (roleCode == null || roleCode.trim().isEmpty()) {
                throw new BizException(ErrorCode.BAD_REQUEST, "roleCodes must not contain blank values");
            }
            normalized.add(roleCode.trim());
        }
        return List.copyOf(normalized);
    }

    private UserWriteResult toUserWriteResult(ManagedUser user) {
        return new UserWriteResult(
                user.id(),
                user.tenantId(),
                user.username(),
                user.displayName(),
                user.email(),
                user.status(),
                user.updatedAt()
        );
    }

    private Map<String, Object> snapshotUser(ManagedUser user, List<String> roleCodes) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", user.id());
        snapshot.put("tenantId", user.tenantId());
        snapshot.put("username", user.username());
        snapshot.put("displayName", user.displayName());
        snapshot.put("email", user.email());
        snapshot.put("status", user.status());
        snapshot.put("roleCodes", roleCodes);
        return snapshot;
    }

    private void throwFeatureNotReady(String action) {
        throw new BizException(ErrorCode.BIZ_ERROR, action + " is not implemented yet");
    }
}
