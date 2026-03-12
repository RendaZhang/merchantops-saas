package com.renda.merchantops.api.messaging;

import com.renda.merchantops.api.context.RequestIdPolicy;
import com.renda.merchantops.api.dto.user.command.UserCreateCommand;
import com.renda.merchantops.api.service.UserCommandService;
import com.renda.merchantops.infra.persistence.entity.ImportJobEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCsvImportProcessorTest {

    @Mock
    private UserCommandService userCommandService;

    @InjectMocks
    private UserCsvImportProcessor userCsvImportProcessor;

    @Test
    void processRowShouldBuildRowRequestIdWithinSchemaLimit() {
        ImportJobEntity job = importJob(1L, 101L, "req-" + "x".repeat(180));

        userCsvImportProcessor.processRow(job, 27, validColumns());

        ArgumentCaptor<String> requestIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(userCommandService).createUser(eq(1L), eq(101L), requestIdCaptor.capture(), any(UserCreateCommand.class));
        assertThat(requestIdCaptor.getValue()).hasSizeLessThanOrEqualTo(RequestIdPolicy.MAX_LENGTH);
        assertThat(requestIdCaptor.getValue()).endsWith("-r27");
    }

    @Test
    void processRowShouldMapDataIntegrityViolationToRowError() {
        ImportJobEntity job = importJob(1L, 101L, "req-import-1");
        when(userCommandService.createUser(eq(1L), eq(101L), any(String.class), any(UserCreateCommand.class)))
                .thenThrow(new DataIntegrityViolationException("value too long for request_id"));

        assertThatThrownBy(() -> userCsvImportProcessor.processRow(job, 2, validColumns()))
                .isInstanceOf(ImportRowProcessingException.class)
                .satisfies(ex -> {
                    ImportRowProcessingException rowException = (ImportRowProcessingException) ex;
                    assertThat(rowException.code()).isEqualTo("ROW_PERSISTENCE_FAILED");
                    assertThat(rowException).hasMessage("failed to persist import row");
                });
    }

    private ImportJobEntity importJob(Long tenantId, Long requestedBy, String requestId) {
        ImportJobEntity job = new ImportJobEntity();
        job.setTenantId(tenantId);
        job.setRequestedBy(requestedBy);
        job.setRequestId(requestId);
        return job;
    }

    private List<String> validColumns() {
        return List.of("alpha", "Alpha User", "alpha@example.com", "abc123", "READ_ONLY");
    }
}
