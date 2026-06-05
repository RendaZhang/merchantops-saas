package com.renda.merchantops.api;

import com.renda.merchantops.api.config.AiProperties;
import com.renda.merchantops.api.config.AuthSessionCleanupProperties;
import com.renda.merchantops.api.config.DotenvBootstrap;
import com.renda.merchantops.api.config.ImportProcessingProperties;
import com.renda.merchantops.api.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.renda.merchantops")
@EnableScheduling
@EnableConfigurationProperties({
        JwtProperties.class,
        ImportProcessingProperties.class,
        AiProperties.class,
        AuthSessionCleanupProperties.class
})
@EntityScan(basePackages = "com.renda.merchantops.infra.persistence.entity")
@EnableJpaRepositories(basePackages = "com.renda.merchantops.infra.repository")
public class MerchantOpsApplication {

    public static void main(String[] args) {
        DotenvBootstrap.loadFromRepositoryRootForLocalDev();
        SpringApplication.run(MerchantOpsApplication.class, args);
    }

}
