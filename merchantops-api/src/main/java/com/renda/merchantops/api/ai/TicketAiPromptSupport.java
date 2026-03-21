package com.renda.merchantops.api.ai;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class TicketAiPromptSupport {

    public static final int COMMENT_HISTORY_LIMIT = 20;
    public static final int OPERATION_LOG_HISTORY_LIMIT = 20;

    private static final int DESCRIPTION_MAX_LENGTH = 600;
    private static final int COMMENT_MAX_LENGTH = 300;
    private static final int OPERATION_LOG_DETAIL_MAX_LENGTH = 200;
    private static final String ELLIPSIS = "...";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private TicketAiPromptSupport() {
    }

    static String buildUserPrompt(TicketAiPromptContext ticket) {
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Ticket core fields\n");
        userPrompt.append("- ticketId: ").append(ticket.id()).append('\n');
        userPrompt.append("- title: ").append(orFallback(ticket.title(), "(none)")).append('\n');
        userPrompt.append("- description: ").append(truncateOrFallback(ticket.description(), DESCRIPTION_MAX_LENGTH, "(none)")).append('\n');
        userPrompt.append("- status: ").append(orFallback(ticket.status(), "(unknown)")).append('\n');
        userPrompt.append("- assignee: ").append(orFallback(ticket.assigneeUsername(), "unassigned")).append('\n');
        userPrompt.append("- createdBy: ").append(orFallback(ticket.createdByUsername(), "(unknown)")).append('\n');
        userPrompt.append("- createdAt: ").append(formatTimestamp(ticket.createdAt())).append('\n');
        userPrompt.append("- updatedAt: ").append(formatTimestamp(ticket.updatedAt())).append('\n');

        appendComments(userPrompt, ticket.comments(), ticket.olderCommentsOmitted());
        appendOperationLogs(userPrompt, ticket.operationLogs(), ticket.olderOperationLogsOmitted());

        return userPrompt.toString();
    }

    private static void appendComments(StringBuilder userPrompt,
                                       List<TicketAiPromptContext.Comment> comments,
                                       boolean olderCommentsOmitted) {
        userPrompt.append('\n').append("Comments\n");
        if (olderCommentsOmitted) {
            userPrompt.append("- earlier comments omitted\n");
        }
        if (comments == null || comments.isEmpty()) {
            userPrompt.append("- none\n");
            return;
        }
        comments.forEach(comment -> userPrompt
                .append("- [")
                .append(formatTimestamp(comment.createdAt()))
                .append("] ")
                .append(orFallback(comment.createdByUsername(), "(unknown)"))
                .append(": ")
                .append(truncateOrFallback(comment.content(), COMMENT_MAX_LENGTH, "(blank)"))
                .append('\n'));
    }

    private static void appendOperationLogs(StringBuilder userPrompt,
                                            List<TicketAiPromptContext.OperationLog> operationLogs,
                                            boolean olderOperationLogsOmitted) {
        userPrompt.append('\n').append("Operation logs\n");
        if (olderOperationLogsOmitted) {
            userPrompt.append("- earlier operation logs omitted\n");
        }
        if (operationLogs == null || operationLogs.isEmpty()) {
            userPrompt.append("- none\n");
            return;
        }
        operationLogs.forEach(log -> userPrompt
                .append("- [")
                .append(formatTimestamp(log.createdAt()))
                .append("] ")
                .append(orFallback(log.operationType(), "(unknown)"))
                .append(" by ")
                .append(orFallback(log.operatorUsername(), "(unknown)"))
                .append(": ")
                .append(truncateOrFallback(log.detail(), OPERATION_LOG_DETAIL_MAX_LENGTH, "(blank)"))
                .append('\n'));
    }

    private static String truncateOrFallback(String value, int maxLength, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength - ELLIPSIS.length()) + ELLIPSIS;
    }

    private static String orFallback(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim();
    }

    private static String formatTimestamp(LocalDateTime value) {
        if (value == null) {
            return "(unknown)";
        }
        return value.format(TIMESTAMP_FORMATTER);
    }
}
