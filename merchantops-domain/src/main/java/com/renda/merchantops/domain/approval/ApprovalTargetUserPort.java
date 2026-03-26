package com.renda.merchantops.domain.approval;

import java.util.Optional;

public interface ApprovalTargetUserPort {

    Optional<ApprovalTargetUser> findForDisable(Long tenantId, Long userId);
}
