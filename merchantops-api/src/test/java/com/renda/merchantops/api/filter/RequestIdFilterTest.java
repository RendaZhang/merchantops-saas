package com.renda.merchantops.api.filter;

import com.renda.merchantops.api.context.RequestIdPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter requestIdFilter = new RequestIdFilter();

    @AfterEach
    void tearDown() {
        MDC.remove(RequestIdFilter.MDC_KEY);
    }

    @Test
    void doFilterShouldNormalizeOversizedRequestIdHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/import-jobs");
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "req-" + "x".repeat(180));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdSeenInsideChain = new AtomicReference<>();

        requestIdFilter.doFilter(request, response, (req, res) ->
                requestIdSeenInsideChain.set(MDC.get(RequestIdFilter.MDC_KEY)));

        String normalizedRequestId = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        assertThat(normalizedRequestId).isNotBlank();
        assertThat(normalizedRequestId.length()).isLessThanOrEqualTo(RequestIdPolicy.MAX_LENGTH);
        assertThat(requestIdSeenInsideChain.get()).isEqualTo(normalizedRequestId);
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }
}
