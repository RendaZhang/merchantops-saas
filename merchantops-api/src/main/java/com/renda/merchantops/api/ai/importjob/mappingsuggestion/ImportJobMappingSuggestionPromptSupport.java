package com.renda.merchantops.api.ai.importjob.mappingsuggestion;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ImportJobMappingSuggestionPromptSupport {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private ImportJobMappingSuggestionPromptSupport() {
    }

    public static String buildUserPrompt(ImportJobMappingSuggestionPromptContext context) {
        StringBuilder userPrompt = new StringBuilder();
        appendCoreFields(userPrompt, context);
        appendCanonicalTargetSchema(userPrompt);
        appendErrorCodeCounts(userPrompt, context.errorCodeCounts());
        appendHeaderSignal(userPrompt, context.headerSignal());
        appendGlobalErrors(userPrompt, context.globalErrors());
        appendSanitizedRows(userPrompt, context.promptWindowErrors());
        return userPrompt.toString();
    }

    private static void appendCoreFields(StringBuilder userPrompt, ImportJobMappingSuggestionPromptContext context) {
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

    private static void appendCanonicalTargetSchema(StringBuilder userPrompt) {
        userPrompt.append('\n').append("Canonical USER_CSV target schema\n");
        userPrompt.append("- username: tenant login identifier\n");
        userPrompt.append("- displayName: operator-facing display name\n");
        userPrompt.append("- email: contact email address\n");
        userPrompt.append("- password: import-time password field\n");
        userPrompt.append("- roleCodes: tenant role-code list\n");
    }

    private static void appendErrorCodeCounts(StringBuilder userPrompt,
                                              List<ImportJobMappingSuggestionPromptContext.ErrorCodeCount> errorCodeCounts) {
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

    private static void appendHeaderSignal(StringBuilder userPrompt,
                                           ImportJobMappingSuggestionPromptContext.HeaderSignalContext headerSignal) {
        userPrompt.append('\n').append("Sanitized header signal\n");
        if (headerSignal == null) {
            userPrompt.append("- none\n");
            return;
        }
        userPrompt.append("- sourceErrorCode: ").append(orFallback(headerSignal.sourceErrorCode(), "(unknown)")).append('\n');
        userPrompt.append("- sourceErrorMessage: ").append(orFallback(headerSignal.sourceErrorMessage(), "(blank)")).append('\n');
        userPrompt.append("- headerColumnCount: ").append(numberOrFallback(headerSignal.headerColumnCount())).append('\n');
        userPrompt.append("- observedColumns:\n");
        if (headerSignal.observedColumns() == null || headerSignal.observedColumns().isEmpty()) {
            userPrompt.append("  - none\n");
            return;
        }
        headerSignal.observedColumns().forEach(column -> userPrompt
                .append("  - headerPosition: ")
                .append(column.headerPosition())
                .append("; headerName: ")
                .append(orFallback(column.headerName(), "(blank)"))
                .append('\n'));
    }

    private static void appendGlobalErrors(StringBuilder userPrompt,
                                           List<ImportJobMappingSuggestionPromptContext.GlobalErrorContext> globalErrors) {
        userPrompt.append('\n').append("Header or global parse-error signals\n");
        if (globalErrors == null || globalErrors.isEmpty()) {
            userPrompt.append("- none\n");
            return;
        }
        globalErrors.forEach(error -> userPrompt
                .append("- errorCode: ")
                .append(orFallback(error.errorCode(), "(unknown)"))
                .append("; errorMessage: ")
                .append(orFallback(error.errorMessage(), "(blank)"))
                .append('\n'));
    }

    private static void appendSanitizedRows(StringBuilder userPrompt,
                                            List<ImportJobMappingSuggestionPromptContext.ErrorRowContext> promptWindowErrors) {
        userPrompt.append('\n').append("Sanitized failure-row prompt window\n");
        userPrompt.append("- ordered by rowNumber ASC, id ASC; limited to first 20 row-level errors; raw row values omitted by design\n");
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

    private static String formatRowSummary(ImportJobMappingSuggestionPromptContext.SanitizedRowSummary rowSummary) {
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
