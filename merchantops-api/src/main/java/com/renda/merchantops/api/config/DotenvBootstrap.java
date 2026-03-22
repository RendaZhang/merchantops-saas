package com.renda.merchantops.api.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DotenvBootstrap {

    private static final String DEFAULT_LOCAL_PROFILE = "dev";

    private DotenvBootstrap() {
    }

    public static void loadFromRepositoryRootForLocalDev() {
        if (!shouldAutoLoadForCurrentLaunch()) {
            return;
        }
        Path repositoryRoot = findRepositoryRoot(Path.of("").toAbsolutePath());
        if (repositoryRoot == null) {
            return;
        }
        Path dotenvPath = repositoryRoot.resolve(".env");
        if (!Files.isRegularFile(dotenvPath)) {
            return;
        }
        try {
            load(dotenvPath);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load .env from " + dotenvPath, ex);
        }
    }

    static boolean shouldAutoLoadForCurrentLaunch() {
        String activeProfiles = firstNonBlank(
                System.getProperty("spring.profiles.active"),
                System.getenv("SPRING_PROFILES_ACTIVE"),
                DEFAULT_LOCAL_PROFILE
        );
        for (String profile : activeProfiles.split(",")) {
            if (DEFAULT_LOCAL_PROFILE.equalsIgnoreCase(profile.trim())) {
                return true;
            }
        }
        return false;
    }

    static Path findRepositoryRoot(Path startPath) {
        Path current = startPath;
        while (current != null) {
            if (isRepositoryRoot(current)) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static boolean isRepositoryRoot(Path candidate) {
        return Files.isRegularFile(candidate.resolve("pom.xml"))
                && Files.isDirectory(candidate.resolve("merchantops-api"))
                && Files.isRegularFile(candidate.resolve("merchantops-api").resolve("pom.xml"));
    }

    static void load(Path dotenvPath) throws IOException {
        List<String> lines = Files.readAllLines(dotenvPath);
        for (String line : lines) {
            Entry entry = parse(line);
            if (entry == null) {
                continue;
            }
            if (System.getProperties().containsKey(entry.key()) || System.getenv(entry.key()) != null) {
                continue;
            }
            System.setProperty(entry.key(), entry.value());
        }
    }

    static Entry parse(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null;
        }
        if (trimmed.startsWith("export ")) {
            trimmed = trimmed.substring("export ".length()).trim();
        }
        int separatorIndex = trimmed.indexOf('=');
        if (separatorIndex <= 0) {
            return null;
        }
        String key = trimmed.substring(0, separatorIndex).trim();
        if (key.isEmpty()) {
            return null;
        }
        String value = trimmed.substring(separatorIndex + 1).trim();
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1);
        }
        return new Entry(key, value);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    record Entry(
            String key,
            String value
    ) {
    }
}
