package com.renda.merchantops.api.contract;

import com.renda.merchantops.api.dto.approval.query.ApprovalRequestResponse;
import com.renda.merchantops.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Approval Requests")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/approval-requests")
public interface ApprovalRequestApi {

    @Operation(summary = "Get approval request detail")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Query successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Approval request not found in current tenant")
    })
    @GetMapping("/{id}")
    ApiResponse<ApprovalRequestResponse> getApprovalRequest(@PathVariable("id") Long id);

    @Operation(summary = "Approve pending request and execute action")
    @PostMapping("/{id}/approve")
    ApiResponse<ApprovalRequestResponse> approve(@PathVariable("id") Long id);

    @Operation(summary = "Reject pending request")
    @PostMapping("/{id}/reject")
    ApiResponse<ApprovalRequestResponse> reject(@PathVariable("id") Long id);
}
