package com.renda.merchantops.api;

import com.renda.merchantops.api.config.ImportProcessingProperties;
import com.renda.merchantops.api.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.renda.merchantops")
@EnableConfigurationProperties({JwtProperties.class, ImportProcessingProperties.class})
@EntityScan(basePackages = "com.renda.merchantops.infra.persistence.entity")
@EnableJpaRepositories(basePackages = "com.renda.merchantops.infra.repository")
public class MerchantOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MerchantOpsApplication.class, args);
    }

}
