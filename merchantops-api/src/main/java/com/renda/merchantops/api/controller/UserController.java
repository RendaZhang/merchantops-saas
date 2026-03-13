package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.contract.UserProfileApi;
import com.renda.merchantops.api.dto.user.UserProfileResponse;
import com.renda.merchantops.api.security.CurrentUser;
import com.renda.merchantops.common.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UserProfileApi {

    @Override
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(new UserProfileResponse(
                currentUser.getUserId(),
                currentUser.getTenantId(),
                currentUser.getTenantCode(),
                currentUser.getUsername(),
                currentUser.getRoles(),
                currentUser.getPermissions()
        ));
    }

}
