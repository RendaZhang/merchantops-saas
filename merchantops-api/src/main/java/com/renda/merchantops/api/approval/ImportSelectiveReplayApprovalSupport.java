package com.renda.merchantops.api.approval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.importjob.ImportSelectiveReplayNormalizer;
import com.renda.merchantops.api.importjob.replay.ImportReplaySourceLoader;
import com.renda.merchantops.domain.approval.ApprovalImportSelectiveReplayPort;
import com.renda.merchantops.domain.approval.ImportSelectiveReplayApprovalCommand;
import com.renda.merchantops.domain.approval.PreparedImportSelectiveReplayApproval;
import com.renda.merchantops.domain.importjob.ImportJobAiInteractionItem;
import com.renda.merchantops.domain.importjob.ImportJobQueryUseCase;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ImportSelectiveReplayApprovalSupport implements ApprovalImportSelectiveReplayPort {

    private static final String INTERACTION_TYPE_FIX_RECOMMENDATION = "FIX_RECOMMENDATION";
    private static final String STATUS_SUCCEEDED = "SUCCEEDED";

    private final ImportReplaySourceLoader importReplaySourceLoader;
    private final ImportSelectiveReplayNormalizer importSelectiveReplayNormalizer;
    private final ImportJobQueryUseCase importJobQueryUseCase;
    private final ObjectMapper objectMapper;

    public ImportSelectiveReplayApprovalSupport(ImportReplaySourceLoader importReplaySourceLoader,
                                                ImportSelectiveReplayNormalizer importSelectiveReplayNormalizer,
                                                ImportJobQueryUseCase importJobQueryUseCase,
                                                ObjectMapper objectMapper) {
        this.importReplaySourceLoader = importReplaySourceLoader;
        this.importSelectiveReplayNormalizer = importSelectiveReplayNormalizer;
        this.importJobQueryUseCase = importJobQueryUseCase;
        this.objectMapper = objectMapper;
    }

    @Override
    public PreparedImportSelectiveReplayApproval prepareProposal(Long tenantId, ImportSelectiveReplayApprovalCommand command) {
        if (command == null || command.sourceJobId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "sourceJobId missing");
        }
        List<String> errorCodes = importSelectiveReplayNormalizer.normalizeSelectedErrorCodes(command.errorCodes());
        importReplaySourceLoader.loadSelectiveFailedRowReplay(tenantId, command.sourceJobId(), errorCodes);
        Long sourceInteractionId = command.sourceInteractionId();
        if (sourceInteractionId != null) {
            ImportJobAiInteractionItem interaction = importJobQueryUseCase.findJobAiInteraction(
                            tenantId,
                            command.sourceJobId(),
                            sourceInteractionId
                    )
                    .orElseThrow(() -> invalidSourceInteraction());
            if (!INTERACTION_TYPE_FIX_RECOMMENDATION.equals(interaction.interactionType())
                    || !STATUS_SUCCEEDED.equals(interaction.status())) {
                throw invalidSourceInteraction();
            }
        }
        String proposalReason = normalizeProposalReason(command.proposalReason());
        return new PreparedImportSelectiveReplayApproval(
                command.sourceJobId(),
                errorCodes,
                sourceInteractionId,
                proposalReason,
                serializePayload(command.sourceJobId(), errorCodes, sourceInteractionId, proposalReason)
        );
    }

    private String serializePayload(Long sourceJobId,
                                    List<String> errorCodes,
                                    Long sourceInteractionId,
                                    String proposalReason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceJobId", sourceJobId);
        payload.put("errorCodes", errorCodes);
        if (sourceInteractionId != null) {
            payload.put("sourceInteractionId", sourceInteractionId);
        }
        if (proposalReason != null) {
            payload.put("proposalReason", proposalReason);
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize import selective replay approval payload", ex);
        }
    }

    private String normalizeProposalReason(String proposalReason) {
        if (!StringUtils.hasText(proposalReason)) {
            return null;
        }
        return proposalReason.trim();
    }

    private BizException invalidSourceInteraction() {
        return new BizException(
                ErrorCode.BAD_REQUEST,
                "sourceInteractionId must reference a succeeded FIX_RECOMMENDATION interaction for the source import job"
        );
    }
}
