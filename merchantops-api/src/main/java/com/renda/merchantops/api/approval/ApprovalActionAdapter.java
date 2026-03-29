package com.renda.merchantops.api.approval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.dto.importjob.command.ImportJobSelectiveReplayRequest;
import com.renda.merchantops.api.importjob.ImportJobReplayService;
import com.renda.merchantops.domain.approval.ApprovalActionPort;
import com.renda.merchantops.domain.approval.ApprovalImportSelectiveReplayPort;
import com.renda.merchantops.domain.approval.ImportSelectiveReplayApprovalCommand;
import com.renda.merchantops.domain.approval.PreparedImportSelectiveReplayApproval;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import com.renda.merchantops.domain.user.UpdateUserStatusCommand;
import com.renda.merchantops.domain.user.UserCommandUseCase;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Component
public class ApprovalActionAdapter implements ApprovalActionPort {

    private final UserCommandUseCase userCommandUseCase;
    private final ImportJobReplayService importJobReplayService;
    private final ApprovalImportSelectiveReplayPort approvalImportSelectiveReplayPort;
    private final ObjectMapper objectMapper;

    public ApprovalActionAdapter(UserCommandUseCase userCommandUseCase,
                                 ImportJobReplayService importJobReplayService,
                                 ApprovalImportSelectiveReplayPort approvalImportSelectiveReplayPort,
                                 ObjectMapper objectMapper) {
        this.userCommandUseCase = userCommandUseCase;
        this.importJobReplayService = importJobReplayService;
        this.approvalImportSelectiveReplayPort = approvalImportSelectiveReplayPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public void disableUser(Long tenantId, Long reviewerId, String requestId, Long userId) {
        userCommandUseCase.updateStatus(
                tenantId,
                reviewerId,
                requestId,
                userId,
                new UpdateUserStatusCommand("DISABLED")
        );
    }

    @Override
    public void replayImportJobSelective(Long tenantId, Long reviewerId, String requestId, Long sourceJobId, String payloadJson) {
        ImportSelectiveReplayApprovalPayload payload = parsePayload(payloadJson);
        PreparedImportSelectiveReplayApproval prepared = approvalImportSelectiveReplayPort.prepareProposal(
                tenantId,
                new ImportSelectiveReplayApprovalCommand(
                        payload.sourceJobId(),
                        payload.errorCodes(),
                        payload.sourceInteractionId(),
                        payload.proposalReason()
                )
        );
        if (!Objects.equals(sourceJobId, prepared.sourceJobId())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "approval payload sourceJobId does not match approval entity");
        }
        importJobReplayService.replayFailedRowsSelective(
                tenantId,
                reviewerId,
                requestId,
                prepared.sourceJobId(),
                new ImportJobSelectiveReplayRequest(prepared.errorCodes())
        );
    }

    private ImportSelectiveReplayApprovalPayload parsePayload(String payloadJson) {
        if (!StringUtils.hasText(payloadJson)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "approval payload missing");
        }
        try {
            return objectMapper.readValue(payloadJson, ImportSelectiveReplayApprovalPayload.class);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.BAD_REQUEST, "approval payload is invalid");
        }
    }
}
