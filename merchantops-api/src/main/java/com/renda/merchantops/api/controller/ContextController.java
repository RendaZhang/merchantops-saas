package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.context.CurrentUserContext;
import com.renda.merchantops.api.context.TenantContext;
import com.renda.merchantops.api.contract.ContextApi;
import com.renda.merchantops.api.dto.context.ContextResponse;
import com.renda.merchantops.common.response.ApiResponse;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContextController implements ContextApi {

    @Override
    public ApiResponse<ContextResponse> context() {
        return ApiResponse.success(new ContextResponse(
                TenantContext.getTenantId(),
                TenantContext.getTenantCode(),
                CurrentUserContext.getUserId(),
                CurrentUserContext.getUsername()
        ));
    }
}
