package com.renda.merchantops.api.user;

import com.renda.merchantops.api.context.ContextAccess;
import com.renda.merchantops.api.context.RequestIdAccess;
import com.renda.merchantops.api.user.UserManagementApi;
import com.renda.merchantops.api.dto.user.command.UserCreateCommand;
import com.renda.merchantops.api.dto.user.command.UserCreateRequest;
import com.renda.merchantops.api.dto.user.command.UserCreateResponse;
import com.renda.merchantops.api.dto.user.command.UserRoleAssignmentCommand;
import com.renda.merchantops.api.dto.user.command.UserRoleAssignmentRequest;
import com.renda.merchantops.api.dto.user.command.UserRoleAssignmentResponse;
import com.renda.merchantops.api.dto.user.command.UserStatusUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserStatusUpdateRequest;
import com.renda.merchantops.api.dto.user.command.UserUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserUpdateRequest;
import com.renda.merchantops.api.dto.user.command.UserWriteResponse;
import com.renda.merchantops.api.dto.user.query.UserDetailResponse;
import com.renda.merchantops.api.dto.user.query.UserPageQuery;
import com.renda.merchantops.api.dto.user.query.UserPageResponse;
import com.renda.merchantops.api.security.RequirePermission;
import com.renda.merchantops.api.user.UserCommandService;
import com.renda.merchantops.api.user.UserQueryService;
import com.renda.merchantops.api.platform.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserManagementController implements UserManagementApi {

    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;

    @Override
    @RequirePermission("USER_READ")
    public ApiResponse<UserPageResponse> listUsers(UserPageQuery query) {
        Long tenantId = ContextAccess.requireTenantId();
        return ApiResponse.success(userQueryService.pageUsers(tenantId, query));
    }

    @Override
    @RequirePermission("USER_READ")
    public ApiResponse<UserDetailResponse> getUserDetail(@PathVariable("id") Long id) {
        Long tenantId = ContextAccess.requireTenantId();
        return ApiResponse.success(userQueryService.getUserDetail(tenantId, id));
    }

    @Override
    @RequirePermission("USER_WRITE")
    public ApiResponse<UserCreateResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        Long tenantId = ContextAccess.requireTenantId();
        Long operatorId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        UserCreateCommand command = new UserCreateCommand(
                request.getUsername(),
                request.getPassword(),
                request.getDisplayName(),
                request.getEmail(),
                request.getRoleCodes()
        );
        return ApiResponse.success(userCommandService.createUser(tenantId, operatorId, requestId, command));
    }

    @Override
    @RequirePermission("USER_WRITE")
    public ApiResponse<UserWriteResponse> updateUser(@PathVariable("id") Long id,
                                                     @Valid @RequestBody UserUpdateRequest request) {
        Long tenantId = ContextAccess.requireTenantId();
        Long operatorId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        UserUpdateCommand command = new UserUpdateCommand(
                request.getDisplayName(),
                request.getEmail()
        );
        return ApiResponse.success(userCommandService.updateUser(tenantId, operatorId, requestId, id, command));
    }

    @Override
    @RequirePermission("USER_WRITE")
    public ApiResponse<UserWriteResponse> updateUserStatus(@PathVariable("id") Long id,
                                                           @Valid @RequestBody UserStatusUpdateRequest request) {
        Long tenantId = ContextAccess.requireTenantId();
        Long operatorId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        UserStatusUpdateCommand command = new UserStatusUpdateCommand(request.getStatus());
        return ApiResponse.success(userCommandService.updateStatus(tenantId, operatorId, requestId, id, command));
    }

    @Override
    @RequirePermission("USER_WRITE")
    public ApiResponse<UserRoleAssignmentResponse> assignRoles(@PathVariable("id") Long id,
                                                               @Valid @RequestBody UserRoleAssignmentRequest request) {
        Long tenantId = ContextAccess.requireTenantId();
        Long operatorId = ContextAccess.requireUserId();
        String requestId = RequestIdAccess.currentRequestId();
        UserRoleAssignmentCommand command = new UserRoleAssignmentCommand(request.getRoleCodes());
        return ApiResponse.success(userCommandService.assignRoles(tenantId, operatorId, requestId, id, command));
    }
}
