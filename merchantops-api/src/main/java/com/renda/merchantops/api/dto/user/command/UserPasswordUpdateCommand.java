package com.renda.merchantops.api.dto.user.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// Password update is isolated from profile/status update to keep the write model explicit.
public class UserPasswordUpdateCommand {

    private String passwordHash;
}
