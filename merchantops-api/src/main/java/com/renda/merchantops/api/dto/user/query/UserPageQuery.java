package com.renda.merchantops.api.dto.user.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPageQuery {

    private Integer page = 0;

    private Integer size = 20;

    private String status;
}
