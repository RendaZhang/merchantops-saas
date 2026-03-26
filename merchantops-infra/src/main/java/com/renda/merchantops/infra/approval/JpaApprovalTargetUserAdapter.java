package com.renda.merchantops.infra.approval;

import com.renda.merchantops.domain.approval.ApprovalTargetUser;
import com.renda.merchantops.domain.approval.ApprovalTargetUserPort;
import com.renda.merchantops.infra.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaApprovalTargetUserAdapter implements ApprovalTargetUserPort {

    private final UserRepository userRepository;

    public JpaApprovalTargetUserAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<ApprovalTargetUser> findForDisable(Long tenantId, Long userId) {
        return userRepository.findByIdAndTenantIdForUpdate(userId, tenantId)
                .map(user -> new ApprovalTargetUser(user.getId(), user.getStatus()));
    }
}
