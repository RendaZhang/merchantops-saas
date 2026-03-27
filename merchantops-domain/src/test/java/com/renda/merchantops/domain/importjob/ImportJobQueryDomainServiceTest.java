package com.renda.merchantops.domain.importjob;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImportJobQueryDomainServiceTest {

    @Test
    void pageJobsShouldNormalizePagingAndTrimFilters() {
        RecordingQueryPort port = new RecordingQueryPort();
        ImportJobQueryDomainService service = new ImportJobQueryDomainService(port);

        service.pageJobs(1L, new ImportJobPageCriteria(-1, 500, " FAILED ", " USER_CSV ", 101L, true));

        assertThat(port.pageCriteria).isEqualTo(new ImportJobPageCriteria(0, 100, "FAILED", "USER_CSV", 101L, true));
    }

    @Test
    void getJobDetailShouldRejectMissingJob() {
        ImportJobQueryDomainService service = new ImportJobQueryDomainService(new RecordingQueryPort());

        assertThatThrownBy(() -> service.getJobDetail(1L, 7001L))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    private static final class RecordingQueryPort implements ImportJobQueryPort {

        private ImportJobPageCriteria pageCriteria;

        @Override
        public ImportJobPageResult pageJobs(Long tenantId, ImportJobPageCriteria criteria) {
            this.pageCriteria = criteria;
            return new ImportJobPageResult(List.of(), criteria.page(), criteria.size(), 0, 0);
        }

        @Override
        public Optional<ImportJobRecord> findJob(Long tenantId, Long importJobId) {
            return Optional.empty();
        }

        @Override
        public ImportJobErrorPageResult pageJobErrors(Long tenantId, Long importJobId, ImportJobErrorPageCriteria criteria) {
            return new ImportJobErrorPageResult(List.of(), criteria.page(), criteria.size(), 0, 0);
        }

        @Override
        public List<ImportJobErrorCount> summarizeErrorCodes(Long tenantId, Long importJobId) {
            return List.of();
        }

        @Override
        public List<ImportJobErrorRecord> listJobErrors(Long tenantId, Long importJobId) {
            return List.of();
        }

        @Override
        public List<ImportJobRecord> findQueuedJobsForRecovery(LocalDateTime createdBefore, int limit) {
            return List.of();
        }

        @Override
        public List<ImportJobRecord> findStaleProcessingJobsForRecovery(LocalDateTime startedBefore, int limit) {
            return List.of();
        }
    }
}
