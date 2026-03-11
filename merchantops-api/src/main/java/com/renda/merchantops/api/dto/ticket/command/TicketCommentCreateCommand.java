package com.renda.merchantops.api.dto.ticket.command;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TicketCommentCreateCommand {

    private String content;
}
