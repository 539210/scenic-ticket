package com.scenicticket.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartupPortabilityTest {
    @Test
    void appLauncherUsesPortableJdkAndMavenDiscovery() throws IOException {
        String script = read("scripts/start-app.cmd");
        String lower = script.toLowerCase(Locale.ROOT);

        assertTrue(lower.contains("scenic_java_home"));
        assertTrue(lower.contains("java_home"));
        assertTrue(lower.contains("where javac.exe"));
        assertTrue(lower.contains("scenic_maven_cmd"));
        assertTrue(lower.contains("maven_home"));
        assertTrue(lower.contains("mvnw.cmd"));
        assertTrue(lower.contains("where mvn.cmd"));
        assertTrue(lower.contains("--check"));
        assertFalse(lower.matches("(?s).*set \\\"[^\\\"]*(?:java|maven)[^\\\"]*=[a-z]:\\\\.*"));
    }

    @Test
    void primaryRequirementsDescribeSupportedDatabaseRanges() throws IOException {
        String readme = read("README.md");
        String requirements = read("docs/需求规格说明书.md");

        assertTrue(readme.contains("MySQL 8.0+") && readme.contains("MongoDB 5.0+"));
        assertTrue(requirements.contains("MySQL 8.0+") && requirements.contains("MongoDB 5.0+"));
        assertFalse(readme.contains("MySQL 8.0.45") || readme.contains("MongoDB 8.3.2"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
