package com.renda.merchantops.api.messaging;

public record ImportJobCreatedEvent(Long jobId, Long tenantId) {
}
