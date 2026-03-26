package com.renda.merchantops.domain.user;

public record UserPageCriteria(
        int page,
        int size,
        String username,
        String status,
        String roleCode
) {
}
