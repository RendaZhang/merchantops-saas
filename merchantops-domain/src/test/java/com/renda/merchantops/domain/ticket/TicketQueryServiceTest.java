package com.renda.merchantops.domain.ticket;

import com.renda.merchantops.domain.shared.error.BizException;
import com.renda.merchantops.domain.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketQueryServiceTest {

    @Mock
    private TicketQueryPort ticketQueryPort;

    @Test
    void pageTicketsShouldNormalizeCriteriaBeforeDelegating() {
        TicketQueryService service = new TicketQueryService(ticketQueryPort);
        when(ticketQueryPort.pageTickets(eq(9L), eq(new TicketPageCriteria(0, 100, "OPEN", 102L, "printer", false))))
                .thenReturn(new TicketPageResult(List.of(), 0, 100, 0, 0));

        service.pageTickets(9L, new TicketPageCriteria(-1, 999, " OPEN ", 102L, " printer ", false));

        ArgumentCaptor<TicketPageCriteria> criteriaCaptor = ArgumentCaptor.forClass(TicketPageCriteria.class);
        verify(ticketQueryPort).pageTickets(eq(9L), criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue()).isEqualTo(new TicketPageCriteria(0, 100, "OPEN", 102L, "printer", false));
    }

    @Test
    void pageTicketsShouldRejectAssigneeAndUnassignedCombination() {
        TicketQueryService service = new TicketQueryService(ticketQueryPort);

        assertThatThrownBy(() -> service.pageTickets(1L, new TicketPageCriteria(0, 10, null, 102L, null, true)))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void pageTicketAiInteractionsShouldThrowNotFoundWhenTicketMissing() {
        TicketQueryService service = new TicketQueryService(ticketQueryPort);
        when(ticketQueryPort.ticketExists(1L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> service.pageTicketAiInteractions(1L, 99L, new TicketAiInteractionPageCriteria(0, 10, null, null)))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
    }
}
