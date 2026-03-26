package com.renda.merchantops.domain.ticket;

public record CreateTicketCommand(
        String title,
        String description
) {
}
