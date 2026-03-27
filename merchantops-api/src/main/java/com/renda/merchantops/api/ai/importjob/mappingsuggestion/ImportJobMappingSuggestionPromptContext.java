package com.renda.merchantops.api.ai.importjob.mappingsuggestion;

import java.time.LocalDateTime;
import java.util.List;

public record ImportJobMappingSuggestionPromptContext(
        Long importJobId,
        String importType,
        String sourceType,
        String sourceFilename,
        String status,
        String errorSummary,
        Integer totalCount,
        Integer successCount,
        Integer failureCount,
        Long sourceJobId,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        List<ErrorCodeCount> errorCodeCounts,
        HeaderSignalContext headerSignal,
        List<GlobalErrorContext> globalErrors,
        List<ErrorRowContext> promptWindowErrors
) {
    public record ErrorCodeCount(
            String errorCode,
            long count
    ) {
    }

    public record HeaderSignalContext(
            String sourceErrorCode,
            String sourceErrorMessage,
            Integer headerColumnCount,
            List<ObservedColumn> observedColumns
    ) {
    }

    public record ObservedColumn(
            String headerName,
            int headerPosition
    ) {
    }

    public record GlobalErrorContext(
            String errorCode,
            String errorMessage
    ) {
    }

    public record ErrorRowContext(
            Integer rowNumber,
            String errorCode,
            String errorMessage,
            SanitizedRowSummary rowSummary
    ) {
    }

    public record SanitizedRowSummary(
            boolean rawPayloadPresent,
            boolean rawPayloadParsed,
            Integer columnCount,
            boolean usernamePresent,
            boolean displayNamePresent,
            boolean emailPresent,
            boolean passwordPresent,
            boolean roleCodesPresent,
            int roleCodeCount
    ) {
    }
}
