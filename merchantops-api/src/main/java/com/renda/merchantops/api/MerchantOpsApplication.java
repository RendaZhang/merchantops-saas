package com.renda.merchantops.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.renda.merchantops")
public class MerchantOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MerchantOpsApplication.class, args);
    }
}
