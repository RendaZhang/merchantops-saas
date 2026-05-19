package com.renda.merchantops.api.dto.ai.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tenant-scoped AI usage and cost summary query")
public class AiInteractionUsageSummaryQuery {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Inclusive start time in ISO-8601 local date-time format", example = "2026-04-01T00:00:00")
    private LocalDateTime from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Inclusive end time in ISO-8601 local date-time format", example = "2026-04-05T23:59:59")
    private LocalDateTime to;

    @Schema(description = "Exact entity type filter. Supported values: TICKET, IMPORT_JOB", example = "TICKET")
    private String entityType;

    @Schema(description = "Exact interaction type filter", example = "SUMMARY")
    private String interactionType;

    @Schema(description = "Exact interaction status filter", example = "SUCCEEDED")
    private String status;
}
