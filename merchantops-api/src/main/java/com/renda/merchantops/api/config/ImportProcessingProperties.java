package com.renda.merchantops.api.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "merchantops.import.processing")
public class ImportProcessingProperties {

    @Min(1)
    private int chunkSize = 100;

    @Min(1)
    private int maxRowsPerJob = 1000;

}
