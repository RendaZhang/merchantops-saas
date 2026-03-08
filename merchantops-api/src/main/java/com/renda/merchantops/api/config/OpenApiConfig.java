package com.renda.merchantops.api.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI merchantOpsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MerchantOps SaaS API")
                        .description("Multi-tenant SaaS backend for merchant operations")
                        .version("v0.0.1")
                        .license(new License().name("Internal Demo")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project README")
                        .url("https://github.com/RendaZhang/merchantops-saas"));
    }
}
