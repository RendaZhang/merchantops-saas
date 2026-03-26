package com.renda.merchantops.api.user;

import com.renda.merchantops.api.user.UserProfileApi;
import com.renda.merchantops.api.dto.user.UserProfileResponse;
import com.renda.merchantops.api.security.CurrentUser;
import com.renda.merchantops.api.platform.response.ApiResponse;
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
