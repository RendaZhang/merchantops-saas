package com.renda.merchantops.api.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImportJobPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(ImportJobMessage message) {
        rabbitTemplate.convertAndSend("import.job.exchange", "import.job.created", message);
    }
}
