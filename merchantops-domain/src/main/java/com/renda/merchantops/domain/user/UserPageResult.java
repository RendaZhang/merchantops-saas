package com.renda.merchantops.domain.user;

import java.util.List;

public record UserPageResult(
        List<UserListItem> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}
