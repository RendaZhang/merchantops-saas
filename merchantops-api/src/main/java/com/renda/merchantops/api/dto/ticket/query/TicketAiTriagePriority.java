package com.renda.merchantops.api.dto.ticket.query;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI-generated ticket triage priority suggestion")
public enum TicketAiTriagePriority {
    LOW,
    MEDIUM,
    HIGH
}
