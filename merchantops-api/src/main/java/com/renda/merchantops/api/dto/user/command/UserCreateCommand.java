package com.renda.merchantops.api.dto.user.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// tenantId is provided by service boundary; id/createdAt/updatedAt are persistence-managed fields.
public class UserCreateCommand {

    private String username;

    private String passwordHash;

    private String displayName;

    private String email;

    private String status;
}
