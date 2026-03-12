package com.renda.merchantops.api.messaging;

public class ImportRowProcessingException extends RuntimeException {

    private final String code;

    public ImportRowProcessingException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
