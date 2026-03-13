package com.renda.merchantops.api.validation;

import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;

public final class PasswordRules {

    public static final String NO_BOUNDARY_WHITESPACE_REGEX = "^\\S(?:.*\\S)?$";
    public static final String NO_BOUNDARY_WHITESPACE_MESSAGE = "password must not start or end with whitespace";

    private PasswordRules() {
    }

    public static void requireNoBoundaryWhitespace(String password) {
        if (hasBoundaryWhitespace(password)) {
            throw new BizException(ErrorCode.BAD_REQUEST, NO_BOUNDARY_WHITESPACE_MESSAGE);
        }
    }

    public static boolean hasBoundaryWhitespace(String password) {
        return password != null
                && !password.isEmpty()
                && (Character.isWhitespace(password.charAt(0))
                || Character.isWhitespace(password.charAt(password.length() - 1)));
    }
}
