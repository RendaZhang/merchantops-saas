package com.renda.merchantops.api.dto.user.command;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserRoleAssignmentCommand {

    private List<String> roleCodes;
}
