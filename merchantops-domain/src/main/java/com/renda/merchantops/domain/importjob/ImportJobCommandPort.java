package com.renda.merchantops.domain.importjob;

import java.util.Optional;

public interface ImportJobCommandPort {

    ImportJobRecord saveJob(ImportJobRecord job);

    Optional<ImportJobRecord> findJobForUpdate(Long tenantId, Long importJobId);

    void saveJobError(ImportJobErrorRecord error);
}
