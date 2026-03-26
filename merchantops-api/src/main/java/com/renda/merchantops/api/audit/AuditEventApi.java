package com.renda.merchantops.api.audit;

import com.renda.merchantops.api.dto.audit.query.AuditEventListResponse;
import com.renda.merchantops.api.platform.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Audit Events")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/audit-events")
public interface AuditEventApi {

    @Operation(summary = "List audit events by entity in current tenant")
    @GetMapping
    ApiResponse<AuditEventListResponse> listByEntity(@RequestParam("entityType") String entityType,
                                                     @RequestParam("entityId") Long entityId);
}
