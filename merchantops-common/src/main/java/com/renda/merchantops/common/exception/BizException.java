package com.renda.merchantops.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(String message) {
        super(message);
        this.errorCode = ErrorCode.BIZ_ERROR;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
