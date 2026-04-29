package com.renda.merchantops.api.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renda.merchantops.api.dto.ticket.command.TicketCommentCreateCommand;
import com.renda.merchantops.api.importjob.ImportJobReplayService;
import com.renda.merchantops.api.ticket.TicketCommandService;
import com.renda.merchantops.domain.approval.ApprovalImportSelectiveReplayPort;
import com.renda.merchantops.domain.approval.ApprovalTicketCommentProposalPort;
import com.renda.merchantops.domain.approval.PreparedTicketCommentApproval;
import com.renda.merchantops.domain.approval.TicketCommentApprovalCommand;
import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import com.renda.merchantops.domain.user.UserCommandUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalActionAdapterTest {

    @Mock
    private UserCommandUseCase userCommandUseCase;

    @Mock
    private ImportJobReplayService importJobReplayService;

    @Mock
    private TicketCommandService ticketCommandService;

    @Mock
    private ApprovalImportSelectiveReplayPort approvalImportSelectiveReplayPort;

    @Mock
    private ApprovalTicketCommentProposalPort approvalTicketCommentProposalPort;

    private ApprovalActionAdapter approvalActionAdapter;

    @BeforeEach
    void setUp() {
        approvalActionAdapter = new ApprovalActionAdapter(
                userCommandUseCase,
                importJobReplayService,
                ticketCommandService,
                approvalImportSelectiveReplayPort,
                approvalTicketCommentProposalPort,
                new ObjectMapper()
        );
    }

    @Test
    void createTicketCommentShouldRevalidateProposalBeforeWritingComment() {
        when(approvalTicketCommentProposalPort.prepareProposal(
                1L,
                new TicketCommentApprovalCommand(301L, "  Reply drafted from AI  ", 9002L)
        )).thenReturn(new PreparedTicketCommentApproval(
                301L,
                "Reply drafted from AI",
                9002L,
                "{\"commentContent\":\"Reply drafted from AI\",\"sourceInteractionId\":9002}",
                "pending-key"
        ));

        approvalActionAdapter.createTicketComment(
                1L,
                105L,
                "approve-ticket-comment-1",
                301L,
                "{\"commentContent\":\"  Reply drafted from AI  \",\"sourceInteractionId\":9002}"
        );

        verify(approvalTicketCommentProposalPort).prepareProposal(
                1L,
                new TicketCommentApprovalCommand(301L, "  Reply drafted from AI  ", 9002L)
        );
        verify(ticketCommandService).addComment(
                1L,
                105L,
                "approve-ticket-comment-1",
                301L,
                new TicketCommentCreateCommand("Reply drafted from AI")
        );
    }

    @Test
    void createTicketCommentShouldStopWhenProposalRevalidationFails() {
        when(approvalTicketCommentProposalPort.prepareProposal(
                1L,
                new TicketCommentApprovalCommand(301L, "Reply drafted from AI", 9002L)
        )).thenThrow(new BizException(ErrorCode.SERVICE_UNAVAILABLE, "ticket comment proposal is disabled"));

        assertThatThrownBy(() -> approvalActionAdapter.createTicketComment(
                1L,
                105L,
                "approve-ticket-comment-flag-disabled",
                301L,
                "{\"commentContent\":\"Reply drafted from AI\",\"sourceInteractionId\":9002}"
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("ticket comment proposal is disabled");

        verify(approvalTicketCommentProposalPort).prepareProposal(
                1L,
                new TicketCommentApprovalCommand(301L, "Reply drafted from AI", 9002L)
        );
        verifyNoInteractions(ticketCommandService);
    }
}
