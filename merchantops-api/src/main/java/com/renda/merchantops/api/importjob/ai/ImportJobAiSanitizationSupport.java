package com.renda.merchantops.api.importjob.ai;

import com.renda.merchantops.api.importjob.ImportCsvSupport;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class ImportJobAiSanitizationSupport {

    private ImportJobAiSanitizationSupport() {
    }

    static SanitizedRowSummary summarizeRowPayload(String rawPayload) {
        if (!StringUtils.hasText(rawPayload)) {
            return new SanitizedRowSummary(false, false, null, false, false, false, false, false, 0);
        }
        try (CSVParser parser = ImportCsvSupport.RAW_PAYLOAD_CSV_FORMAT.parse(new StringReader(rawPayload))) {
            List<CSVRecord> records = parser.getRecords();
            if (records.size() != 1) {
                return new SanitizedRowSummary(true, false, null, false, false, false, false, false, 0);
            }
            CSVRecord record = records.getFirst();
            String username = column(record, 0);
            String displayName = column(record, 1);
            String email = column(record, 2);
            String password = column(record, 3);
            String roleCodes = column(record, 4);
            return new SanitizedRowSummary(
                    true,
                    true,
                    record.size(),
                    StringUtils.hasText(username),
                    StringUtils.hasText(displayName),
                    StringUtils.hasText(email),
                    StringUtils.hasText(password),
                    StringUtils.hasText(roleCodes),
                    countRoleCodes(roleCodes)
            );
        } catch (IOException | RuntimeException ex) {
            return new SanitizedRowSummary(true, false, null, false, false, false, false, false, 0);
        }
    }

    static SanitizedHeaderSignal summarizeHeaderSignal(String rawPayload) {
        if (!StringUtils.hasText(rawPayload)) {
            return new SanitizedHeaderSignal(false, false, null, List.of());
        }
        try (CSVParser parser = ImportCsvSupport.RAW_PAYLOAD_CSV_FORMAT.parse(new StringReader(rawPayload))) {
            List<CSVRecord> records = parser.getRecords();
            if (records.size() != 1) {
                return new SanitizedHeaderSignal(true, false, null, List.of());
            }
            CSVRecord record = records.getFirst();
            List<ObservedHeaderColumn> observedColumns = new ArrayList<>();
            for (int index = 0; index < record.size(); index++) {
                String headerName = normalizeHeaderToken(record.get(index));
                if (headerName != null) {
                    observedColumns.add(new ObservedHeaderColumn(headerName, index + 1));
                }
            }
            return new SanitizedHeaderSignal(true, true, record.size(), List.copyOf(observedColumns));
        } catch (IOException | RuntimeException ex) {
            return new SanitizedHeaderSignal(true, false, null, List.of());
        }
    }

    static String normalizeHeaderToken(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value
                .replace("\uFEFF", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private static String column(CSVRecord record, int index) {
        if (record == null || index < 0 || index >= record.size()) {
            return null;
        }
        return record.get(index);
    }

    private static int countRoleCodes(String roleCodes) {
        if (!StringUtils.hasText(roleCodes)) {
            return 0;
        }
        return (int) Arrays.stream(roleCodes.split("\\|", -1))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .count();
    }

    record SanitizedRowSummary(
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

    record SanitizedHeaderSignal(
            boolean rawPayloadPresent,
            boolean rawPayloadParsed,
            Integer headerColumnCount,
            List<ObservedHeaderColumn> observedColumns
    ) {
    }

    record ObservedHeaderColumn(
            String headerName,
            int headerPosition
    ) {
    }
}
