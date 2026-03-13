package com.renda.merchantops.api.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordHashGenerator {

    private PasswordHashGenerator() {
    }

    public static void main(String[] args) {
        if (args.length != 1 || args[0].isBlank()) {
            throw new IllegalArgumentException("Usage: PasswordHashGenerator <plain-password>");
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode(args[0]));
    }
}
