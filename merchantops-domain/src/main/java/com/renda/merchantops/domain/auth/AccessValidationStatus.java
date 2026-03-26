package com.renda.merchantops.domain.auth;

public enum AccessValidationStatus {
    USER_ACTIVE,
    USER_INACTIVE,
    USER_MISSING,
    CLAIMS_STALE
}
