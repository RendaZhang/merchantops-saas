package com.renda.merchantops.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Current-user auth session list")
public record AuthSessionListResponse(
        @Schema(description = "Auth sessions for the authenticated user in the current tenant, newest first")
        List<AuthSessionListItemResponse> items
) {
}
