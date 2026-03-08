package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.dto.EchoRequest;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dev")
public class DevController {

    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "module", "merchantops-api"
        ));
    }

    @PostMapping("/echo")
    public ApiResponse<Map<String, Object>> echo(@Valid @RequestBody EchoRequest request) {
        return ApiResponse.success(Map.of(
                "message", request.getMessage()
        ));
    }

    @GetMapping("/biz-error")
    public ApiResponse<Void> bizError() {
        throw new BizException(ErrorCode.BIZ_ERROR, "demo business exception");
    }
}
