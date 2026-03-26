package com.renda.merchantops.api.importjob.messaging;

public record ImportJobCreatedEvent(Long jobId, Long tenantId) {
}
