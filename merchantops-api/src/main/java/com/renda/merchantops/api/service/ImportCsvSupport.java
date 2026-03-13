package com.renda.merchantops.api.service;

import org.apache.commons.csv.CSVFormat;

import java.util.List;

public final class ImportCsvSupport {

    public static final CSVFormat IMPORT_CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setIgnoreEmptyLines(false)
            .build();

    public static final CSVFormat RAW_PAYLOAD_CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setRecordSeparator("\n")
            .build();

    public static final List<String> USER_CSV_HEADERS = List.of(
            "username",
            "displayName",
            "email",
            "password",
            "roleCodes"
    );

    private ImportCsvSupport() {
    }
}
