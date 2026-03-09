package com.renda.merchantops.api.service;

import com.renda.merchantops.api.context.ContextAccess;
import com.renda.merchantops.api.dto.user.UserSummaryResponse;
import com.renda.merchantops.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;

    public List<UserSummaryResponse> listCurrentTenantUsers() {
        Long tenantId = ContextAccess.requireTenantId();

        return userRepository.findAllByTenantIdOrderByIdAsc(tenantId)
                .stream()
                .map(user -> new UserSummaryResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        user.getEmail(),
                        user.getStatus()
                ))
                .toList();
    }

}
