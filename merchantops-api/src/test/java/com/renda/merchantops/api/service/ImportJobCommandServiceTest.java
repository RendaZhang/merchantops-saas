package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.importjob.command.ImportJobCreateRequest;
import com.renda.merchantops.api.dto.importjob.query.ImportJobDetailResponse;
import com.renda.merchantops.api.messaging.ImportJobPublisher;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.ImportJobEntity;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.repository.ImportJobRepository;
import com.renda.merchantops.infra.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportJobCommandServiceTest {

    @Mock
    private ImportJobRepository importJobRepository;
    @Mock
    private ImportFileStorageService importFileStorageService;
    @Mock
    private ImportJobPublisher importJobPublisher;
    @Mock
    private ImportJobQueryService importJobQueryService;
    @Mock
    private AuditEventService auditEventService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ImportJobCommandService importJobCommandService;

    @Test
    void createJobShouldPersistQueuedJobAndPublishMessage() {
        UserEntity user = new UserEntity();
        user.setId(101L);
        user.setTenantId(1L);
        when(userRepository.findByIdAndTenantId(101L, 1L)).thenReturn(Optional.of(user));
        when(importFileStorageService.store(any(), any())).thenReturn("1/key.csv");
        when(importJobRepository.save(any(ImportJobEntity.class))).thenAnswer(invocation -> {
            ImportJobEntity entity = invocation.getArgument(0);
            entity.setId(7001L);
            return entity;
        });
        when(importJobQueryService.toDetail(any())).thenReturn(new ImportJobDetailResponse(
                7001L, 1L, "USER_CSV", "CSV", "users.csv", "1/key.csv", "QUEUED", 101L, "req-1",
                0, 0, 0, null, null, null, null, List.of()
        ));

        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("user_csv");
        MockMultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv", "name,email\na,b".getBytes());

        ImportJobDetailResponse response = importJobCommandService.createJob(1L, 101L, "req-1", request, file);

        assertThat(response.id()).isEqualTo(7001L);
        ArgumentCaptor<ImportJobEntity> captor = ArgumentCaptor.forClass(ImportJobEntity.class);
        verify(importJobRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("QUEUED");
        assertThat(captor.getValue().getImportType()).isEqualTo("USER_CSV");
        verify(importJobPublisher).publish(any());
    }

    @Test
    void createJobShouldRejectInvalidImportType() {
        ImportJobCreateRequest request = new ImportJobCreateRequest();
        request.setImportType("ticket_csv");
        MockMultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv", "a".getBytes());

        assertThatThrownBy(() -> importJobCommandService.createJob(1L, 101L, "req-1", request, file))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                });
    }
}
