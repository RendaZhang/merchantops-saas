package com.renda.merchantops.api.context;

import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class RequestIdPolicy {

    public static final int MAX_LENGTH = 128;

    private static final int HASH_LENGTH = 12;

    private RequestIdPolicy() {
    }

    public static String normalizeOrGenerate(String requestId) {
        String normalized = normalizeNullable(requestId);
        return normalized == null ? UUID.randomUUID().toString() : normalized;
    }

    public static String requireNormalized(String requestId) {
        String normalized = normalizeNullable(requestId);
        if (normalized == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "request id missing");
        }
        return normalized;
    }

    public static String createImportRowRequestId(String requestId, int rowNumber) {
        String baseRequestId = requireNormalized(requestId);
        String suffix = "-r" + rowNumber;
        if (suffix.length() >= MAX_LENGTH) {
            return shorten(suffix, MAX_LENGTH);
        }
        return shorten(baseRequestId, MAX_LENGTH - suffix.length()) + suffix;
    }

    private static String normalizeNullable(String requestId) {
        if (!StringUtils.hasText(requestId)) {
            return null;
        }
        return shorten(requestId.trim(), MAX_LENGTH);
    }

    private static String shorten(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }

        String hash = shortHash(value);
        if (maxLength <= hash.length()) {
            return hash.substring(0, maxLength);
        }

        int prefixLength = maxLength - hash.length() - 1;
        if (prefixLength <= 0) {
            return hash.substring(0, maxLength);
        }
        return value.substring(0, prefixLength) + "-" + hash;
    }

    private static String shortHash(String value) {
        return DigestUtils.md5DigestAsHex(value.getBytes(StandardCharsets.UTF_8))
                .substring(0, HASH_LENGTH);
    }
}
