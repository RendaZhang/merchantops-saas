package com.renda.merchantops.api.config;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthSessionCleanupPropertiesTest {

    @Test
    void shouldDefaultToEnabledSevenDayRetentionHourlyDelayAndBatchSizeHundred() {
        AuthSessionCleanupProperties properties = new AuthSessionCleanupProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getRetentionSeconds()).isEqualTo(604800);
        assertThat(properties.getFixedDelayMs()).isEqualTo(3600000);
        assertThat(properties.getBatchSize()).isEqualTo(100);
    }

    @Test
    void shouldRejectNonPositiveRetentionDelayAndBatchSize() {
        AuthSessionCleanupProperties properties = new AuthSessionCleanupProperties();
        properties.setRetentionSeconds(0);
        properties.setFixedDelayMs(0);
        properties.setBatchSize(0);

        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(properties))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactlyInAnyOrder("retentionSeconds", "fixedDelayMs", "batchSize");
        }
    }
}
