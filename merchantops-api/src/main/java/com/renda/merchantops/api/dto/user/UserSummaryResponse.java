package com.renda.merchantops.api.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSummaryResponse {

    private Long id;

    private String username;

    private String displayName;

    private String email;

    private String status;

}
