package com.renda.merchantops.api.auth;

import com.renda.merchantops.api.config.AuthSessionCleanupProperties;
import com.renda.merchantops.domain.auth.AuthSessionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthSessionCleanupScheduler {

    private final AuthSessionUseCase authSessionUseCase;
    private final AuthSessionCleanupProperties authSessionCleanupProperties;

    @Scheduled(
            fixedDelayString = "${merchantops.auth.session.cleanup.fixed-delay-ms:3600000}",
            initialDelayString = "${merchantops.auth.session.cleanup.fixed-delay-ms:3600000}"
    )
    public void runCleanup() {
        cleanupOnce();
    }

    public int cleanupOnce() {
        return cleanupOnce(Instant.now());
    }

    int cleanupOnce(Instant now) {
        if (!authSessionCleanupProperties.isEnabled()) {
            return 0;
        }
        int deletedCount = authSessionUseCase.cleanupExpiredOrRevokedSessions(
                now,
                authSessionCleanupProperties.getRetentionSeconds(),
                authSessionCleanupProperties.getBatchSize()
        );
        if (deletedCount > 0) {
            log.info("deleted {} auth_session rows older than retention window", deletedCount);
        }
        return deletedCount;
    }
}
