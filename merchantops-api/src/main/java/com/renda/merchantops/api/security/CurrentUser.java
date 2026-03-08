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

}
