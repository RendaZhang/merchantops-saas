package com.renda.merchantops.api.dto.user.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// Only mutable business fields live here. username, tenantId, id, and createdAt are not updateable.
public class UserUpdateCommand {

    private String displayName;

    private String email;

    private String status;
}
