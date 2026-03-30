package com.renda.merchantops.api.approval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.domain.approval.ApprovalTicketCommentProposalPort;
import com.renda.merchantops.domain.approval.PreparedTicketCommentApproval;
import com.renda.merchantops.domain.approval.TicketCommentApprovalCommand;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import com.renda.merchantops.domain.ticket.TicketAiInteractionItem;
import com.renda.merchantops.domain.ticket.TicketQueryUseCase;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TicketCommentApprovalSupport implements ApprovalTicketCommentProposalPort {

    private static final String INTERACTION_TYPE_REPLY_DRAFT = "REPLY_DRAFT";
    private static final String STATUS_SUCCEEDED = "SUCCEEDED";
    private static final int MAX_COMMENT_LENGTH = 2000;

    private final TicketQueryUseCase ticketQueryUseCase;
    private final ObjectMapper objectMapper;

    public TicketCommentApprovalSupport(TicketQueryUseCase ticketQueryUseCase, ObjectMapper objectMapper) {
        this.ticketQueryUseCase = ticketQueryUseCase;
        this.objectMapper = objectMapper;
    }

    @Override
    public PreparedTicketCommentApproval prepareProposal(Long tenantId, TicketCommentApprovalCommand command) {
        if (command == null || command.ticketId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "ticketId missing");
        }
        ticketQueryUseCase.getTicketDetail(tenantId, command.ticketId());
        String commentContent = normalizeCommentContent(command.commentContent());
        Long sourceInteractionId = command.sourceInteractionId();
        if (sourceInteractionId != null) {
            TicketAiInteractionItem interaction = ticketQueryUseCase.findTicketAiInteraction(
                            tenantId,
                            command.ticketId(),
                            sourceInteractionId
                    )
                    .orElseThrow(this::invalidSourceInteraction);
            if (!INTERACTION_TYPE_REPLY_DRAFT.equals(interaction.interactionType())
                    || !STATUS_SUCCEEDED.equals(interaction.status())) {
                throw invalidSourceInteraction();
            }
        }
        return new PreparedTicketCommentApproval(
                command.ticketId(),
                commentContent,
                sourceInteractionId,
                serializePayload(commentContent, sourceInteractionId)
        );
    }

    private String normalizeCommentContent(String commentContent) {
        if (!StringUtils.hasText(commentContent)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "commentContent must not be blank");
        }
        String trimmed = commentContent.trim();
        if (trimmed.length() > MAX_COMMENT_LENGTH) {
            throw new BizException(ErrorCode.BAD_REQUEST, "commentContent length must be less than or equal to 2000");
        }
        return trimmed;
    }

    private String serializePayload(String commentContent, Long sourceInteractionId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("commentContent", commentContent);
        if (sourceInteractionId != null) {
            payload.put("sourceInteractionId", sourceInteractionId);
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize ticket comment approval payload", ex);
        }
    }

    private BizException invalidSourceInteraction() {
        return new BizException(
                ErrorCode.BAD_REQUEST,
                "sourceInteractionId must reference a succeeded REPLY_DRAFT interaction for the source ticket"
        );
    }
}
