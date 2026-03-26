package com.renda.merchantops.api.audit;

import com.renda.merchantops.api.context.ContextAccess;
import com.renda.merchantops.api.audit.AuditEventApi;
import com.renda.merchantops.api.dto.audit.query.AuditEventListResponse;
import com.renda.merchantops.api.security.RequirePermission;
import com.renda.merchantops.api.audit.AuditEventService;
import com.renda.merchantops.api.platform.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuditEventController implements AuditEventApi {

    private final AuditEventService auditEventService;

    @Override
    @RequirePermission("USER_READ")
    public ApiResponse<AuditEventListResponse> listByEntity(@RequestParam("entityType") String entityType,
                                                            @RequestParam("entityId") Long entityId) {
        Long tenantId = ContextAccess.requireTenantId();
        return ApiResponse.success(auditEventService.listByEntity(tenantId, entityType, entityId));
    }
}
