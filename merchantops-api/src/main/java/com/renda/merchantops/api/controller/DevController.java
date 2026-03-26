package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.contract.DevApi;
import com.renda.merchantops.api.dto.EchoRequest;
import com.renda.merchantops.api.dto.dev.DevPingResponse;
import com.renda.merchantops.api.dto.dev.EchoResponse;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import com.renda.merchantops.api.platform.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DevController implements DevApi {

    @Override
    public ApiResponse<DevPingResponse> ping() {
        return ApiResponse.success(new DevPingResponse("UP", "merchantops-api"));
    }

    @Override
    public ApiResponse<EchoResponse> echo(@Valid @RequestBody EchoRequest request) {
        return ApiResponse.success(new EchoResponse(request.getMessage()));
    }

    @Override
    public ApiResponse<Void> bizError() {
        throw new BizException(ErrorCode.BIZ_ERROR, "demo business exception");
    }
}
