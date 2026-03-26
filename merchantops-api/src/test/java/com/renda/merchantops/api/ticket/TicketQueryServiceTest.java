package com.renda.merchantops.api.ticket;

import com.renda.merchantops.api.ai.ticket.summary.TicketSummaryPromptBuilder;
import com.renda.merchantops.api.dto.ticket.query.TicketAiInteractionPageQuery;
import com.renda.merchantops.api.dto.ticket.query.TicketAiInteractionPageResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketDetailResponse;
import com.renda.merchantops.api.dto.ticket.query.TicketPageQuery;
import com.renda.merchantops.api.dto.ticket.query.TicketPageResponse;
import com.renda.merchantops.domain.ticket.TicketAiInteractionItem;
import com.renda.merchantops.domain.ticket.TicketAiInteractionPageCriteria;
import com.renda.merchantops.domain.ticket.TicketAiInteractionPageResult;
import com.renda.merchantops.domain.ticket.TicketCommentView;
import com.renda.merchantops.domain.ticket.TicketDetail;
import com.renda.merchantops.domain.ticket.TicketListItem;
import com.renda.merchantops.domain.ticket.TicketOperationLogView;
import com.renda.merchantops.domain.ticket.TicketPageCriteria;
import com.renda.merchantops.domain.ticket.TicketPageResult;
import com.renda.merchantops.domain.ticket.TicketPromptContext;
import com.renda.merchantops.domain.ticket.TicketPromptWindowPolicy;
import com.renda.merchantops.domain.ticket.TicketQueryUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketQueryServiceTest {

    @Mock
    private TicketQueryUseCase ticketQueryUseCase;

    @InjectMocks
    private TicketQueryService ticketQueryService;

    @Test
    void pageTicketsShouldMapDomainResultAndForwardCriteria() {
        TicketPageQuery query = new TicketPageQuery(-1, 999, " OPEN ", 102L, " printer ", false);
        when(ticketQueryUseCase.pageTickets(eq(1L), any(TicketPageCriteria.class))).thenReturn(new TicketPageResult(
                List.of(new TicketListItem(
                        11L,
                        "POS printer offline",
                        "OPEN",
                        102L,
                        "ops",
                        LocalDateTime.of(2026, 3, 11, 10, 0),
                        LocalDateTime.of(2026, 3, 11, 10, 5)
                )),
                0,
                100,
                1,
                1
        ));

        TicketPageResponse response = ticketQueryService.pageTickets(1L, query);

        ArgumentCaptor<TicketPageCriteria> criteriaCaptor = ArgumentCaptor.forClass(TicketPageCriteria.class);
        verify(ticketQueryUseCase).pageTickets(eq(1L), criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().page()).isEqualTo(-1);
        assertThat(criteriaCaptor.getValue().size()).isEqualTo(999);
        assertThat(criteriaCaptor.getValue().status()).isEqualTo(" OPEN ");
        assertThat(criteriaCaptor.getValue().assigneeId()).isEqualTo(102L);
        assertThat(criteriaCaptor.getValue().keyword()).isEqualTo(" printer ");
        assertThat(criteriaCaptor.getValue().unassignedOnly()).isFalse();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().getFirst().getAssigneeUsername()).isEqualTo("ops");
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(100);
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
    }

    @Test
    void pageTicketAiInteractionsShouldMapDomainPageAndForwardCriteria() {
        TicketAiInteractionPageQuery query = new TicketAiInteractionPageQuery(-1, 999, " SUMMARY ", " SUCCEEDED ");
        when(ticketQueryUseCase.pageTicketAiInteractions(eq(1L), eq(11L), any(TicketAiInteractionPageCriteria.class)))
                .thenReturn(new TicketAiInteractionPageResult(
                        List.of(new TicketAiInteractionItem(
                                9001L,
                                "SUMMARY",
                                "SUCCEEDED",
                                "Issue: Printer cable replacement is in progress under ops.",
                                "ticket-summary-v1",
                                "gpt-4.1-mini",
                                412L,
                                "ticket-ai-summary-req-1",
                                120,
                                52,
                                172,
                                1900L,
                                LocalDateTime.of(2026, 3, 22, 8, 30)
                        )),
                        0,
                        100,
                        1,
                        1
                ));

        TicketAiInteractionPageResponse response = ticketQueryService.pageTicketAiInteractions(1L, 11L, query);

        ArgumentCaptor<TicketAiInteractionPageCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(TicketAiInteractionPageCriteria.class);
        verify(ticketQueryUseCase).pageTicketAiInteractions(eq(1L), eq(11L), criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().page()).isEqualTo(-1);
        assertThat(criteriaCaptor.getValue().size()).isEqualTo(999);
        assertThat(criteriaCaptor.getValue().interactionType()).isEqualTo(" SUMMARY ");
        assertThat(criteriaCaptor.getValue().status()).isEqualTo(" SUCCEEDED ");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().getFirst().getId()).isEqualTo(9001L);
        assertThat(response.getItems().getFirst().getRequestId()).isEqualTo("ticket-ai-summary-req-1");
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(100);
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
    }

    @Test
    void getTicketDetailShouldMapCommentsAndOperationLogs() {
        when(ticketQueryUseCase.getTicketDetail(1L, 11L)).thenReturn(new TicketDetail(
                11L,
                1L,
                "POS printer offline",
                "desc",
                "IN_PROGRESS",
                102L,
                "ops",
                101L,
                "admin",
                LocalDateTime.of(2026, 3, 11, 10, 0),
                LocalDateTime.of(2026, 3, 11, 10, 5),
                List.of(new TicketCommentView(
                        31L,
                        11L,
                        "Printer restarted",
                        102L,
                        "ops",
                        LocalDateTime.of(2026, 3, 11, 10, 10)
                )),
                List.of(
                        new TicketOperationLogView(
                                41L,
                                "CREATED",
                                "ticket created",
                                101L,
                                "admin",
                                LocalDateTime.of(2026, 3, 11, 10, 0)
                        ),
                        new TicketOperationLogView(
                                42L,
                                "ASSIGNED",
                                "assigned to ops",
                                101L,
                                "admin",
                                LocalDateTime.of(2026, 3, 11, 10, 2)
                        )
                )
        ));

        TicketDetailResponse response = ticketQueryService.getTicketDetail(1L, 11L);

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getAssigneeUsername()).isEqualTo("ops");
        assertThat(response.getCreatedByUsername()).isEqualTo("admin");
        assertThat(response.getComments()).hasSize(1);
        assertThat(response.getComments().getFirst().getCreatedByUsername()).isEqualTo("ops");
        assertThat(response.getOperationLogs()).hasSize(2);
        assertThat(response.getOperationLogs().get(1).getDetail()).isEqualTo("assigned to ops");
    }

    @Test
    void promptBuilderShouldStillRespectPromptWindowMarkersWithDomainContext() {
        TicketPromptContext context = new TicketPromptContext(
                11L,
                1L,
                "POS printer offline",
                "d".repeat(650),
                "IN_PROGRESS",
                "ops",
                "admin",
                LocalDateTime.of(2026, 3, 11, 10, 0),
                LocalDateTime.of(2026, 3, 11, 10, 5),
                java.util.stream.LongStream.rangeClosed(102L, 121L)
                        .mapToObj(id -> new TicketPromptContext.Comment(
                                id,
                                "c".repeat(320) + "-" + id,
                                "ops",
                                LocalDateTime.of(2026, 3, 11, 10, 10)
                        ))
                        .toList(),
                true,
                java.util.stream.LongStream.rangeClosed(202L, 221L)
                        .mapToObj(id -> new TicketPromptContext.OperationLog(
                                id,
                                "STATUS_CHANGED",
                                "l".repeat(220) + "-" + id,
                                "admin",
                                LocalDateTime.of(2026, 3, 11, 10, 15)
                        ))
                        .toList(),
                true
        );

        String userPrompt = new TicketSummaryPromptBuilder().build("ticket-summary-v1", context).userPrompt();

        assertThat(context.comments()).hasSize(TicketPromptWindowPolicy.COMMENT_HISTORY_LIMIT);
        assertThat(context.operationLogs()).hasSize(TicketPromptWindowPolicy.OPERATION_LOG_HISTORY_LIMIT);
        assertThat(userPrompt).contains("- earlier comments omitted");
        assertThat(userPrompt).contains("- earlier operation logs omitted");
        assertThat(userPrompt).contains("description: " + "d".repeat(597) + "...");
        assertThat(userPrompt).contains(": " + "c".repeat(297) + "...");
        assertThat(userPrompt).contains(": " + "l".repeat(197) + "...");
    }
}
