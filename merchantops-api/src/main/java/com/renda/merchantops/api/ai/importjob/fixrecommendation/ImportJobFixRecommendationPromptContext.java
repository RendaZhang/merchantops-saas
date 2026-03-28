package com.renda.merchantops.api.ai.importjob.fixrecommendation;

import java.time.LocalDateTime;
import java.util.List;

public record ImportJobFixRecommendationPromptContext(
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
        List<ErrorGroupContext> groundedErrorGroups
) {
    public record ErrorCodeCount(
            String errorCode,
            long count
    ) {
    }

    public record ErrorGroupContext(
            String errorCode,
            long affectedRowsEstimate,
            int promptWindowSampleCount,
            List<String> sampleErrorMessages,
            List<ErrorRowContext> sampleRows
    ) {
    }

    public record ErrorRowContext(
            Integer rowNumber,
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
