package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.context.ContextAccess;
import com.renda.merchantops.api.contract.UserManagementApi;
import com.renda.merchantops.api.dto.user.command.UserCreateCommand;
import com.renda.merchantops.api.dto.user.command.UserCreateRequest;
import com.renda.merchantops.api.dto.user.command.UserCreateResponse;
import com.renda.merchantops.api.dto.user.command.UserStatusUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserStatusUpdateRequest;
import com.renda.merchantops.api.dto.user.command.UserUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserUpdateRequest;
import com.renda.merchantops.api.dto.user.command.UserWriteResponse;
import com.renda.merchantops.api.dto.user.query.UserPageQuery;
import com.renda.merchantops.api.dto.user.query.UserPageResponse;
import com.renda.merchantops.api.security.RequirePermission;
import com.renda.merchantops.api.service.UserCommandService;
import com.renda.merchantops.api.service.UserQueryService;
import com.renda.merchantops.common.response.ApiResponse;
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
    @RequirePermission("USER_WRITE")
    public ApiResponse<UserCreateResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        Long tenantId = ContextAccess.requireTenantId();
        UserCreateCommand command = new UserCreateCommand(
                request.getUsername(),
                request.getPassword(),
                request.getDisplayName(),
                request.getEmail(),
                request.getRoleCodes()
        );
        return ApiResponse.success(userCommandService.createUser(tenantId, command));
    }

    @Override
    @RequirePermission("USER_WRITE")
    public ApiResponse<UserWriteResponse> updateUser(@PathVariable("id") Long id,
                                                     @Valid @RequestBody UserUpdateRequest request) {
        Long tenantId = ContextAccess.requireTenantId();
        UserUpdateCommand command = new UserUpdateCommand(
                request.getDisplayName(),
                request.getEmail()
        );
        return ApiResponse.success(userCommandService.updateUser(tenantId, id, command));
    }

    @Override
    @RequirePermission("USER_WRITE")
    public ApiResponse<UserWriteResponse> updateUserStatus(@PathVariable("id") Long id,
                                                           @Valid @RequestBody UserStatusUpdateRequest request) {
        Long tenantId = ContextAccess.requireTenantId();
        UserStatusUpdateCommand command = new UserStatusUpdateCommand(request.getStatus());
        return ApiResponse.success(userCommandService.updateStatus(tenantId, id, command));
    }
}
