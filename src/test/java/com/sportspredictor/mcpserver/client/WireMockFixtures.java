package com.sportspredictor.mcpserver.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Shared utility for loading WireMock JSON fixture files from the test classpath. */
public final class WireMockFixtures {

    private WireMockFixtures() {}

    /** Loads a fixture file from {@code src/test/resources/} by classpath path. */
    public static String load(String path) {
        try (var stream = WireMockFixtures.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Fixture not found: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load fixture: " + path, e);
        }
    }
}
