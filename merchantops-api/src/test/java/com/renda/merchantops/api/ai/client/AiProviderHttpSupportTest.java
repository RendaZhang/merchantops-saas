package com.renda.merchantops.api.ai.client;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderHttpSupportTest {

    @Test
    void isTimeoutShouldRecognizeNestedSocketTimeoutException() {
        ResourceAccessException exception = new ResourceAccessException(
                "I/O error",
                new SocketTimeoutException("Read timed out")
        );

        assertThat(AiProviderHttpSupport.isTimeout(exception)).isTrue();
    }

    @Test
    void isTimeoutShouldRecognizeHttpTimeoutException() {
        RuntimeException exception = new RuntimeException(new HttpTimeoutException("request timed out"));

        assertThat(AiProviderHttpSupport.isTimeout(exception)).isTrue();
    }

    @Test
    void isTimeoutShouldIgnoreNonTimeoutFailures() {
        RuntimeException exception = new RuntimeException("connection reset by peer");

        assertThat(AiProviderHttpSupport.isTimeout(exception)).isFalse();
    }
}
