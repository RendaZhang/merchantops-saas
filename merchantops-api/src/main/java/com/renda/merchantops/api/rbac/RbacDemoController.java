package com.renda.merchantops.api.rbac;

import com.renda.merchantops.api.dto.rbac.RbacActionResponse;
import com.renda.merchantops.api.platform.response.ApiResponse;
import com.renda.merchantops.api.security.RequirePermission;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RbacDemoController implements RbacApi {

    @Override
    @RequirePermission("USER_READ")
    public ApiResponse<RbacActionResponse> readUsers() {
        return ApiResponse.success(new RbacActionResponse("read users", "allowed"));
    }

    @Override
    @RequirePermission("USER_WRITE")
    public ApiResponse<RbacActionResponse> manageUsers() {
        return ApiResponse.success(new RbacActionResponse("manage users", "allowed"));
    }
}
