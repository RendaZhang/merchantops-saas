package com.renda.merchantops.api.approval;

import com.renda.merchantops.domain.approval.ApprovalActionPort;
import com.renda.merchantops.domain.user.UpdateUserStatusCommand;
import com.renda.merchantops.domain.user.UserCommandUseCase;
import org.springframework.stereotype.Component;

@Component
public class UserDisableApprovalActionAdapter implements ApprovalActionPort {

    private final UserCommandUseCase userCommandUseCase;

    public UserDisableApprovalActionAdapter(UserCommandUseCase userCommandUseCase) {
        this.userCommandUseCase = userCommandUseCase;
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
}
