package com.renda.merchantops.api.security;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class CurrentUser implements Serializable {

    private Long userId;

    private Long tenantId;

    private String tenantCode;

    private String username;

    private List<String> roles;

    private List<String> permissions;

    private String sessionId;

    public CurrentUser(Long userId,
                       Long tenantId,
                       String tenantCode,
                       String username,
                       List<String> roles,
                       List<String> permissions) {
        this(userId, tenantId, tenantCode, username, roles, permissions, null);
    }

}
