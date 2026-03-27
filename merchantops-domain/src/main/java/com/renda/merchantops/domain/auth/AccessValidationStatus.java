package com.renda.merchantops.domain.auth;

public enum AccessValidationStatus {
    USER_ACTIVE,
    TENANT_INACTIVE,
    USER_INACTIVE,
    USER_MISSING,
    CLAIMS_STALE
}
