package com.renda.merchantops.api.context;

import com.renda.merchantops.api.filter.RequestIdFilter;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.UUID;

public final class RequestIdAccess {

    private RequestIdAccess() {
    }

    public static String currentRequestId() {
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (StringUtils.hasText(requestId)) {
            return requestId;
        }
        return UUID.randomUUID().toString();
    }
}
