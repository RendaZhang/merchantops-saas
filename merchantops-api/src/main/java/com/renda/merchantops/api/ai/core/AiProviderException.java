package com.renda.merchantops.api.ai.core;

import lombok.Getter;

@Getter
public class AiProviderException extends RuntimeException {

    private final AiProviderFailureType failureType;

    public AiProviderException(AiProviderFailureType failureType, String message) {
        super(message);
        this.failureType = failureType;
    }
}
