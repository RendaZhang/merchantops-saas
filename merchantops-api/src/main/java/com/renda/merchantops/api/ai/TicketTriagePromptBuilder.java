package com.renda.merchantops.api.ai;

import com.renda.merchantops.api.dto.ticket.query.TicketCommentResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketDetailResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketOperationLogResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class TicketTriagePromptBuilder {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final String SYSTEM_PROMPT = """
            You are MerchantOps Ticket Triage Copilot.
            Review only the tenant-scoped ticket facts present in the provided context.
            Do not invent missing history, hidden causes, or unsupported actions.
            Return:
            - classification: a short issue label string, preferably uppercase snake case
            - priority: exactly one of LOW, MEDIUM, HIGH
            - reasoning: a short human-readable explanation grounded in the current ticket facts
            Do not suggest assignees, approvals, workflow mutations, or automatic write-back.
            """;

    public TicketTriagePrompt build(String promptVersion, TicketDetailResponse ticket) {
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Ticket core fields\n");
        userPrompt.append("- ticketId: ").append(ticket.getId()).append('\n');
        userPrompt.append("- title: ").append(orFallback(ticket.getTitle(), "(none)")).append('\n');
        userPrompt.append("- description: ").append(orFallback(ticket.getDescription(), "(none)")).append('\n');
        userPrompt.append("- status: ").append(orFallback(ticket.getStatus(), "(unknown)")).append('\n');
        userPrompt.append("- assignee: ").append(orFallback(ticket.getAssigneeUsername(), "unassigned")).append('\n');
        userPrompt.append("- createdBy: ").append(orFallback(ticket.getCreatedByUsername(), "(unknown)")).append('\n');
        userPrompt.append("- createdAt: ").append(formatTimestamp(ticket.getCreatedAt())).append('\n');
        userPrompt.append("- updatedAt: ").append(formatTimestamp(ticket.getUpdatedAt())).append('\n');

        appendComments(userPrompt, ticket.getComments());
        appendOperationLogs(userPrompt, ticket.getOperationLogs());

        return new TicketTriagePrompt(promptVersion, SYSTEM_PROMPT, userPrompt.toString());
    }

    private void appendComments(StringBuilder userPrompt, List<TicketCommentResponse> comments) {
        userPrompt.append('\n').append("Comments\n");
        if (comments == null || comments.isEmpty()) {
            userPrompt.append("- none\n");
            return;
        }
        comments.forEach(comment -> userPrompt
                .append("- [")
                .append(formatTimestamp(comment.getCreatedAt()))
                .append("] ")
                .append(orFallback(comment.getCreatedByUsername(), "(unknown)"))
                .append(": ")
                .append(orFallback(comment.getContent(), "(blank)"))
                .append('\n'));
    }

    private void appendOperationLogs(StringBuilder userPrompt, List<TicketOperationLogResponse> operationLogs) {
        userPrompt.append('\n').append("Operation logs\n");
        if (operationLogs == null || operationLogs.isEmpty()) {
            userPrompt.append("- none\n");
            return;
        }
        operationLogs.forEach(log -> userPrompt
                .append("- [")
                .append(formatTimestamp(log.getCreatedAt()))
                .append("] ")
                .append(orFallback(log.getOperationType(), "(unknown)"))
                .append(" by ")
                .append(orFallback(log.getOperatorUsername(), "(unknown)"))
                .append(": ")
                .append(orFallback(log.getDetail(), "(blank)"))
                .append('\n'));
    }

    private String orFallback(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim();
    }

    private String formatTimestamp(LocalDateTime value) {
        if (value == null) {
            return "(unknown)";
        }
        return value.format(TIMESTAMP_FORMATTER);
    }
}
