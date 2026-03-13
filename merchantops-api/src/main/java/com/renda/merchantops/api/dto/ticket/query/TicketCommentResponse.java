package com.renda.merchantops.api.dto.ticket.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(description = "Ticket comment")
public class TicketCommentResponse {

    @Schema(description = "Comment ID", example = "11")
    private Long id;

    @Schema(description = "Ticket ID", example = "1")
    private Long ticketId;

    @Schema(description = "Comment content", example = "Checked store network. Printer recovered after restart.")
    private String content;

    @Schema(description = "Comment author user ID", example = "2")
    private Long createdBy;

    @Schema(description = "Comment author username", example = "ops")
    private String createdByUsername;

    @Schema(description = "Created time", example = "2026-03-11T10:15:00")
    private LocalDateTime createdAt;
}
