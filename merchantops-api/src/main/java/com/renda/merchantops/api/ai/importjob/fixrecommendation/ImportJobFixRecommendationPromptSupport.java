package com.renda.merchantops.api.ai.importjob.fixrecommendation;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ImportJobFixRecommendationPromptSupport {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private ImportJobFixRecommendationPromptSupport() {
    }

    public static String buildUserPrompt(ImportJobFixRecommendationPromptContext context) {
        StringBuilder userPrompt = new StringBuilder();
        appendCoreFields(userPrompt, context);
        appendErrorCodeCounts(userPrompt, context.errorCodeCounts());
        appendGroundedErrorGroups(userPrompt, context.groundedErrorGroups());
        return userPrompt.toString();
    }

    private static void appendCoreFields(StringBuilder userPrompt, ImportJobFixRecommendationPromptContext context) {
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
    }

    private static void appendErrorCodeCounts(StringBuilder userPrompt,
                                              List<ImportJobFixRecommendationPromptContext.ErrorCodeCount> errorCodeCounts) {
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

    private static void appendGroundedErrorGroups(StringBuilder userPrompt,
                                                  List<ImportJobFixRecommendationPromptContext.ErrorGroupContext> groundedErrorGroups) {
        userPrompt.append('\n').append("Grounded row-level failure groups\n");
        userPrompt.append("- grouped locally by row-level errorCode; ordered by local affected-row estimate; raw row values omitted by design\n");
        if (groundedErrorGroups == null || groundedErrorGroups.isEmpty()) {
            userPrompt.append("- none\n");
            return;
        }
        groundedErrorGroups.forEach(group -> {
            userPrompt.append("- errorCode: ").append(orFallback(group.errorCode(), "(unknown)")).append('\n');
            userPrompt.append("  affectedRowsEstimate: ").append(group.affectedRowsEstimate()).append('\n');
            userPrompt.append("  promptWindowSampleCount: ").append(group.promptWindowSampleCount()).append('\n');
            userPrompt.append("  sampleErrorMessages:\n");
            appendNestedStringList(userPrompt, group.sampleErrorMessages());
            userPrompt.append("  sampleRows:\n");
            appendSampleRows(userPrompt, group.sampleRows());
        });
    }

    private static void appendNestedStringList(StringBuilder userPrompt, List<String> values) {
        if (values == null || values.isEmpty()) {
            userPrompt.append("    - none\n");
            return;
        }
        values.forEach(value -> userPrompt
                .append("    - ")
                .append(orFallback(value, "(blank)"))
                .append('\n'));
    }

    private static void appendSampleRows(StringBuilder userPrompt,
                                         List<ImportJobFixRecommendationPromptContext.ErrorRowContext> sampleRows) {
        if (sampleRows == null || sampleRows.isEmpty()) {
            userPrompt.append("    - none\n");
            return;
        }
        sampleRows.forEach(row -> userPrompt
                .append("    - rowNumber: ")
                .append(row.rowNumber() == null ? "(unknown)" : row.rowNumber())
                .append("; errorMessage: ")
                .append(orFallback(row.errorMessage(), "(blank)"))
                .append("; structuralSummary: ")
                .append(formatRowSummary(row.rowSummary()))
                .append('\n'));
    }

    private static String formatRowSummary(ImportJobFixRecommendationPromptContext.SanitizedRowSummary rowSummary) {
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
