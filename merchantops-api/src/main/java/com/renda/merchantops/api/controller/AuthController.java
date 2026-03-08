package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.dto.auth.LoginRequest;
import com.renda.merchantops.api.dto.auth.LoginResponse;
import com.renda.merchantops.api.service.AuthService;
import com.renda.merchantops.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login with tenantCode, username and password")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

}
