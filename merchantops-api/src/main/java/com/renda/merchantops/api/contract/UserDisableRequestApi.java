package com.renda.merchantops.api.contract;

import com.renda.merchantops.api.dto.approval.query.ApprovalRequestResponse;
import com.renda.merchantops.api.platform.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "User Management")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/users")
public interface UserDisableRequestApi {

    @Operation(summary = "Create disable approval request for user")
    @PostMapping("/{id}/disable-requests")
    ApiResponse<ApprovalRequestResponse> createUserDisableRequest(@PathVariable("id") Long id);
}
