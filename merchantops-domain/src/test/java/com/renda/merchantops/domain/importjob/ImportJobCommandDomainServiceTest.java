package com.renda.merchantops.domain.importjob;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImportJobCommandDomainServiceTest {

    @Test
    void createQueuedJobShouldNormalizeSupportedImportTypeAndInitializeQueuedCounters() {
        RecordingCommandPort port = new RecordingCommandPort();
        ImportJobCommandDomainService service = new ImportJobCommandDomainService(port);

        ImportJobRecord saved = service.createQueuedJob(
                1L,
                101L,
                " req-import-1 ",
                new NewImportJobDraft(" user_csv ", " csv ", " users.csv ", " 1/key.csv ", null)
        );

        assertThat(saved.importType()).isEqualTo("USER_CSV");
        assertThat(saved.sourceType()).isEqualTo("CSV");
        assertThat(saved.sourceFilename()).isEqualTo("users.csv");
        assertThat(saved.storageKey()).isEqualTo("1/key.csv");
        assertThat(saved.status()).isEqualTo("QUEUED");
        assertThat(saved.totalCount()).isZero();
        assertThat(saved.successCount()).isZero();
        assertThat(saved.failureCount()).isZero();
        assertThat(saved.requestId()).isEqualTo("req-import-1");
    }

    @Test
    void createQueuedJobShouldRejectUnsupportedImportType() {
        ImportJobCommandDomainService service = new ImportJobCommandDomainService(new RejectOnlyCommandPort());

        assertThatThrownBy(() -> service.createQueuedJob(
                1L,
                101L,
                "req-import-1",
                new NewImportJobDraft("ticket_csv", "CSV", "users.csv", "1/key.csv", null)
        )).isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    private static final class RecordingCommandPort implements ImportJobCommandPort {

        @Override
        public ImportJobRecord saveJob(ImportJobRecord job) {
            return new ImportJobRecord(
                    7001L,
                    job.tenantId(),
                    job.importType(),
                    job.sourceType(),
                    job.sourceFilename(),
                    job.storageKey(),
                    job.sourceJobId(),
                    job.status(),
                    job.requestedBy(),
                    job.requestId(),
                    job.totalCount(),
                    job.successCount(),
                    job.failureCount(),
                    job.errorSummary(),
                    job.createdAt(),
                    job.startedAt(),
                    job.finishedAt()
            );
        }

        @Override
        public java.util.Optional<ImportJobRecord> findJobForUpdate(Long tenantId, Long importJobId) {
            return java.util.Optional.empty();
        }

        @Override
        public void saveJobError(ImportJobErrorRecord error) {
        }
    }

    private static final class RejectOnlyCommandPort implements ImportJobCommandPort {

        @Override
        public ImportJobRecord saveJob(ImportJobRecord job) {
            return job;
        }

        @Override
        public java.util.Optional<ImportJobRecord> findJobForUpdate(Long tenantId, Long importJobId) {
            return java.util.Optional.empty();
        }

        @Override
        public void saveJobError(ImportJobErrorRecord error) {
        }
    }
}
