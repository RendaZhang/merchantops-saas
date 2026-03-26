package com.renda.merchantops.api.importjob;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
class ImportStoredFileCleanupSupport {

    private final ImportFileStorageService importFileStorageService;

    void registerRollbackCleanup(String storageKey) {
        if (!StringUtils.hasText(storageKey) || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteStoredFileQuietly(storageKey);
                }
            }
        });
    }

    private void deleteStoredFileQuietly(String storageKey) {
        try {
            importFileStorageService.delete(storageKey);
        } catch (Exception ex) {
            log.warn("failed to delete import file {} after rollback", storageKey, ex);
        }
    }
}
