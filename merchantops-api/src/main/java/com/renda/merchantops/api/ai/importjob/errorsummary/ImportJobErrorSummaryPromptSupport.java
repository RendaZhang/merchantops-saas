package com.renda.merchantops.api.ai.importjob.errorsummary;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ImportJobErrorSummaryPromptSupport {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private ImportJobErrorSummaryPromptSupport() {
    }

    public static String buildUserPrompt(ImportJobErrorSummaryPromptContext context) {
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Import job core fields\n");
        userPrompt.append("- importJobId: ").append(context.importJobId()).append('\n');
        userPrompt.append("- importType: ").append(orFallback(context.importType(), "(unknown)")).append('\n');
        userPrompt.append("- sourceType: ").append(orFallback(context.sourceType(), "(unknown)")).append('\n');
        userPrompt.append("- sourceFilename: ").append(orFallback(context.sourceFilename(), "(unknown)")).append('\n');
        userPrompt.append("- sourceJobId: ").append(context.sourceJobId() == null ? "(none)" : context.sourceJobId()).append('\n');
        userPrompt.append("- status: ").append(orFallback(context.status(), "(unknown)")).append('\n');
        userPrompt.append("- totalCount: ").append(numberOrFallback(context.totalCount())).append('\n');
        userPrompt.append("- successCount: ").append(numberOrFallback(context.successCount())).append('\n');
        userPrompt.append("- failureCount: ").append(numberOrFallback(context.failureCount())).append('\n');
        userPrompt.append("- errorSummary: ").append(orFallback(context.errorSummary(), "(none)")).append('\n');
        userPrompt.append("- createdAt: ").append(formatTimestamp(context.createdAt())).append('\n');
        userPrompt.append("- startedAt: ").append(formatTimestamp(context.startedAt())).append('\n');
        userPrompt.append("- finishedAt: ").append(formatTimestamp(context.finishedAt())).append('\n');

        appendErrorCodeCounts(userPrompt, context.errorCodeCounts());
        appendSanitizedRows(userPrompt, context.promptWindowErrors());

        return userPrompt.toString();
    }

    private static void appendErrorCodeCounts(StringBuilder userPrompt,
                                              List<ImportJobErrorSummaryPromptContext.ErrorCodeCount> errorCodeCounts) {
        userPrompt.append('\n').append("Error code counts\n");
        if (errorCodeCounts == null || errorCodeCounts.isEmpty()) {
            userPrompt.append("- none\n");
            return;
        }
        errorCodeCounts.forEach(count -> userPrompt
                .append("- ")
                .append(orFallback(count.errorCode(), "(unknown)"))
                .append(": ")
                .append(count.count())
                .append('\n'));
    }

    private static void appendSanitizedRows(StringBuilder userPrompt,
                                            List<ImportJobErrorSummaryPromptContext.ErrorRowContext> promptWindowErrors) {
        userPrompt.append('\n').append("Sanitized failure-row prompt window\n");
        userPrompt.append("- ordered by rowNumber ASC, id ASC; limited to first 20 rows; raw CSV values omitted by design\n");
        if (promptWindowErrors == null || promptWindowErrors.isEmpty()) {
            userPrompt.append("- none\n");
            return;
        }
        promptWindowErrors.forEach(error -> userPrompt
                .append("- rowNumber: ")
                .append(error.rowNumber() == null ? "(global)" : error.rowNumber())
                .append("; errorCode: ")
                .append(orFallback(error.errorCode(), "(unknown)"))
                .append("; errorMessage: ")
                .append(orFallback(error.errorMessage(), "(blank)"))
                .append("; structuralSummary: ")
                .append(formatRowSummary(error.rowSummary()))
                .append('\n'));
    }

    private static String formatRowSummary(ImportJobErrorSummaryPromptContext.SanitizedRowSummary rowSummary) {
        if (rowSummary == null) {
            return "rawPayloadPresent=false";
        }
        return "rawPayloadPresent=" + rowSummary.rawPayloadPresent()
                + ", rawPayloadParsed=" + rowSummary.rawPayloadParsed()
                + ", columnCount=" + numberOrFallback(rowSummary.columnCount())
                + ", usernamePresent=" + rowSummary.usernamePresent()
                + ", displayNamePresent=" + rowSummary.displayNamePresent()
                + ", emailPresent=" + rowSummary.emailPresent()
                + ", passwordPresent=" + rowSummary.passwordPresent()
                + ", roleCodesPresent=" + rowSummary.roleCodesPresent()
                + ", roleCodeCount=" + rowSummary.roleCodeCount();
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

    private static String numberOrFallback(Integer value) {
        return value == null ? "(unknown)" : value.toString();
    }
}
