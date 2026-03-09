package com.renda.merchantops.api.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI merchantOpsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MerchantOps SaaS API")
                        .description("Multi-tenant merchant operations backend. All business endpoints return ApiResponse.")
                        .version("v0.0.1")
                        .license(new License().name("Internal Demo")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Input JWT accessToken from /api/v1/auth/login")))
                .addTagsItem(new Tag().name("Authentication").description("Login and token acquisition"))
                .addTagsItem(new Tag().name("Health").description("Health and readiness endpoints"))
                .addTagsItem(new Tag().name("Development").description("Development-only public test endpoints"))
                .addTagsItem(new Tag().name("Context").description("Current authenticated tenant/user context"))
                .addTagsItem(new Tag().name("User Profile").description("Current logged-in user information"))
                .addTagsItem(new Tag().name("User Management").description("Tenant-scoped user query endpoints"))
                .addTagsItem(new Tag().name("RBAC").description("Permission-protected RBAC demonstration endpoints"))
                .externalDocs(new ExternalDocumentation()
                        .description("Project API reference")
                        .url("https://github.com/RendaZhang/merchantops-saas/blob/main/docs/reference/api-docs.md"));
    }
}
