package com.renda.merchantops.api.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "merchantops.auth.session.cleanup")
public class AuthSessionCleanupProperties {

    private boolean enabled = true;

    @Min(1)
    private long retentionSeconds = 604800;

    @Min(1)
    private long fixedDelayMs = 3600000;

    @Min(1)
    private int batchSize = 100;
}
