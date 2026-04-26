package com.renda.merchantops.api.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class AiClientConfig {

    @Bean
    public RetryTemplate aiProviderRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(1)
                .build();
    }

    @Bean
    public ToolCallingManager aiProviderToolCallingManager() {
        return DefaultToolCallingManager.builder()
                .observationRegistry(ObservationRegistry.NOOP)
                .toolCallbackResolver(new StaticToolCallbackResolver(List.of()))
                .toolExecutionExceptionProcessor(DefaultToolExecutionExceptionProcessor.builder().build())
                .build();
    }
}
