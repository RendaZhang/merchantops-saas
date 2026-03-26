package com.renda.merchantops.domain.user;

import java.util.List;

public record AssignUserRolesCommand(
        List<String> roleCodes
) {
}
