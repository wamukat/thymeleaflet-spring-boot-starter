package io.github.wamukat.thymeleaflet.testsupport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class FixtureResources {

    private FixtureResources() {
    }

    public static String text(String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath cannot be null");
        ClassLoader classLoader = FixtureResources.class.getClassLoader();
        try (var inputStream = classLoader.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Fixture resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read fixture resource: " + resourcePath, exception);
        }
    }
}
