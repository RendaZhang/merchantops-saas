package com.renda.merchantops.api.controller;

import com.renda.merchantops.api.contract.HealthApi;
import com.renda.merchantops.api.dto.health.HealthResponse;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController implements HealthApi {

    @Override
    public HealthResponse health() {
        return new HealthResponse("UP", "merchantops-saas");
    }
}
