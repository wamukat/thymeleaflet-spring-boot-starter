package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StorybookPropertiesTest {

    @Test
    void templatePaths_requiresAtLeastOne() {
        StorybookProperties.ResourceConfig config = new StorybookProperties.ResourceConfig();
        assertThrows(IllegalArgumentException.class, () -> config.setTemplatePaths(Collections.emptyList()));
    }

    @Test
    void templatePaths_rejectsTooMany() {
        StorybookProperties.ResourceConfig config = new StorybookProperties.ResourceConfig();
        List<String> tooMany = List.of("/t1/", "/t2/", "/t3/", "/t4/", "/t5/", "/t6/");
        assertThrows(IllegalArgumentException.class, () -> config.setTemplatePaths(tooMany));
    }

    @Test
    void stylesheets_rejectsTooMany() {
        StorybookProperties.ResourceConfig config = new StorybookProperties.ResourceConfig();
        List<String> tooMany = List.of(
            "/1.css", "/2.css", "/3.css", "/4.css", "/5.css",
            "/6.css", "/7.css", "/8.css", "/9.css", "/10.css", "/11.css"
        );
        assertThrows(IllegalArgumentException.class, () -> config.setStylesheets(tooMany));
    }

    @Test
    void cacheDuration_requiresPositive() {
        StorybookProperties.ResourceConfig config = new StorybookProperties.ResourceConfig();
        assertThrows(IllegalArgumentException.class, () -> config.setCacheDurationSeconds(0));
        assertThrows(IllegalArgumentException.class, () -> config.setCacheDurationSeconds(-1));
        assertDoesNotThrow(() -> config.setCacheDurationSeconds(1));
    }
}
