package com.renda.merchantops.api.context;

import com.renda.merchantops.api.filter.RequestIdFilter;
import org.slf4j.MDC;

public final class RequestIdAccess {

    private RequestIdAccess() {
    }

    public static String currentRequestId() {
        return RequestIdPolicy.normalizeOrGenerate(MDC.get(RequestIdFilter.MDC_KEY));
    }
}
