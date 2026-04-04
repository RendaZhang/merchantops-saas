package com.renda.merchantops.domain.approval;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public final class ApprovalPendingRequestKeyPolicy {

    private ApprovalPendingRequestKeyPolicy() {
    }

    public static String userStatusDisableKey(Long tenantId, Long userId) {
        return ApprovalActionTypes.USER_STATUS_DISABLE + ":" + requireValue(tenantId, "tenantId") + ":" + requireValue(userId, "userId");
    }

    public static String importJobSelectiveReplayKey(Long tenantId, Long sourceJobId, List<String> errorCodes) {
        String canonicalErrorCodes = String.join("|", requireValue(errorCodes, "errorCodes"));
        return ApprovalActionTypes.IMPORT_JOB_SELECTIVE_REPLAY
                + ":" + requireValue(tenantId, "tenantId")
                + ":" + requireValue(sourceJobId, "sourceJobId")
                + ":" + md5Hex(canonicalErrorCodes);
    }

    public static String ticketCommentCreateKey(Long tenantId, Long ticketId, String commentContent) {
        return ApprovalActionTypes.TICKET_COMMENT_CREATE
                + ":" + requireValue(tenantId, "tenantId")
                + ":" + requireValue(ticketId, "ticketId")
                + ":" + md5Hex(requireValue(commentContent, "commentContent"));
    }

    private static <T> T requireValue(T value, String fieldName) {
        return Objects.requireNonNull(value, fieldName + " must not be null");
    }

    private static String md5Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("MD5 algorithm unavailable", ex);
        }
    }
}
