package io.github.wamukat.thymeleaflet.infrastructure.configuration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class StorybookPropertiesUsageRestrictionTest {

    private static final Path MAIN_JAVA_ROOT = Path.of("src/main/java/io/github/wamukat/thymeleaflet");
    private static final String STORYBOOK_PROPERTIES_IMPORT =
        "import io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookProperties;";

    @Test
    void storybookPropertiesImport_shouldBeLimitedToConfigurationLayer() throws IOException {
        List<String> violations = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(MAIN_JAVA_ROOT)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        if (!content.contains(STORYBOOK_PROPERTIES_IMPORT)) {
                            return;
                        }
                        String normalizedPath = MAIN_JAVA_ROOT.relativize(path).toString().replace('\\', '/');
                        if (normalizedPath.equals("infrastructure/configuration/StorybookAutoConfiguration.java")) {
                            return;
                        }
                        if (normalizedPath.equals("infrastructure/configuration/ResolvedStorybookConfig.java")) {
                            return;
                        }
                        violations.add(normalizedPath);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        }

        assertThat(violations)
            .withFailMessage("StorybookProperties import is restricted to configuration layer, but found in: %s", violations)
            .isEmpty();
    }
}
