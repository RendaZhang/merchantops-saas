package com.renda.merchantops.api.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DotenvBootstrapTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        System.clearProperty("FROM_ENV");
        System.clearProperty("KEEP_ME");
        System.clearProperty("QUOTED");
        System.clearProperty("SINGLE_QUOTED");
        System.clearProperty("spring.profiles.active");
    }

    @Test
    void findRepositoryRootShouldSearchCurrentDirectoryThenParents() throws Exception {
        Path parent = Files.createDirectories(tempDir.resolve("repo"));
        Path child = Files.createDirectories(parent.resolve("merchantops-api"));
        Files.writeString(parent.resolve("pom.xml"), "<project/>");
        Files.writeString(child.resolve("pom.xml"), "<project/>");

        assertThat(DotenvBootstrap.findRepositoryRoot(child)).isEqualTo(parent);
    }

    @Test
    void shouldAutoLoadForCurrentLaunchShouldOnlyEnableDevProfile() {
        System.setProperty("spring.profiles.active", "prod");
        assertThat(DotenvBootstrap.shouldAutoLoadForCurrentLaunch()).isFalse();

        System.setProperty("spring.profiles.active", "prod,dev");
        assertThat(DotenvBootstrap.shouldAutoLoadForCurrentLaunch()).isTrue();
    }

    @Test
    void loadShouldIgnoreCommentsTrimQuotesAndNotOverrideExistingProperties() throws Exception {
        Path dotenv = tempDir.resolve(".env");
        Files.writeString(dotenv, """
                # comment
                FROM_ENV=loaded
                KEEP_ME=dotenv
                QUOTED="quoted value"
                export SINGLE_QUOTED='single quoted'
                INVALID_LINE
                """);
        System.setProperty("KEEP_ME", "already-set");

        DotenvBootstrap.load(dotenv);

        assertThat(System.getProperty("FROM_ENV")).isEqualTo("loaded");
        assertThat(System.getProperty("KEEP_ME")).isEqualTo("already-set");
        assertThat(System.getProperty("QUOTED")).isEqualTo("quoted value");
        assertThat(System.getProperty("SINGLE_QUOTED")).isEqualTo("single quoted");
    }
}
