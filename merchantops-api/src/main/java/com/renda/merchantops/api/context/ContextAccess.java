package com.renda.merchantops.api.context;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;

public final class ContextAccess {

    private ContextAccess() {
    }

    public static Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "tenant context missing");
        }
        return tenantId;
    }

    public static Long requireUserId() {
        Long userId = CurrentUserContext.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "user context missing");
        }
        return userId;
    }

}
