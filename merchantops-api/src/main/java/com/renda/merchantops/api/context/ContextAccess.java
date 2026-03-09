package com.renda.merchantops.api.context;

import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;

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
