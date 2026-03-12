package com.renda.merchantops.api.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImportJobMessagingConfig {

    public static final String IMPORT_JOB_QUEUE = "import.job.queue";

    @Bean
    public Queue importJobQueue() {
        return new Queue(IMPORT_JOB_QUEUE, true);
    }

    @Bean
    public DirectExchange importJobExchange() {
        return new DirectExchange("import.job.exchange", true, false);
    }

    @Bean
    public Binding importJobBinding(Queue importJobQueue, DirectExchange importJobExchange) {
        return BindingBuilder.bind(importJobQueue).to(importJobExchange).with("import.job.created");
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
