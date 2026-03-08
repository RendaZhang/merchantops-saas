package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.dto.EchoRequest;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dev")
public class DevController {

    @Operation(summary = "Ping dev endpoint")
    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "module", "merchantops-api"
        ));
    }

    @Operation(summary = "Echo request body")
    @PostMapping("/echo")
    public ApiResponse<Map<String, Object>> echo(@Valid @RequestBody EchoRequest request) {
        return ApiResponse.success(Map.of(
                "message", request.getMessage()
        ));
    }

    @Operation(summary = "Trigger demo business exception")
    @GetMapping("/biz-error")
    public ApiResponse<Void> bizError() {
        throw new BizException(ErrorCode.BIZ_ERROR, "demo business exception");
    }
}
