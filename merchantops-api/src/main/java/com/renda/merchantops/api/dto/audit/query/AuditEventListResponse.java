package com.renda.merchantops.api.dto.audit.query;

import java.util.List;

public record AuditEventListResponse(
        List<AuditEventResponse> items
) {
}
