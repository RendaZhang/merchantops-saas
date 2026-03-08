package com.renda.merchantops.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS("SUCCESS", "ok"),

    BAD_REQUEST("BAD_REQUEST", "bad request"),
    VALIDATION_ERROR("VALIDATION_ERROR", "validation failed"),
    UNAUTHORIZED("UNAUTHORIZED", "unauthorized"),
    FORBIDDEN("FORBIDDEN", "forbidden"),
    NOT_FOUND("NOT_FOUND", "resource not found"),
    BIZ_ERROR("BIZ_ERROR", "business error"),
    INTERNAL_ERROR("INTERNAL_ERROR", "internal server error");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
