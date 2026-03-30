package com.renda.merchantops.domain.approval;

import java.util.Locale;

public final class ApprovalActionTypes {

    public static final String USER_STATUS_DISABLE = "USER_STATUS_DISABLE";
    public static final String IMPORT_JOB_SELECTIVE_REPLAY = "IMPORT_JOB_SELECTIVE_REPLAY";
    public static final String TICKET_COMMENT_CREATE = "TICKET_COMMENT_CREATE";

    private ApprovalActionTypes() {
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
