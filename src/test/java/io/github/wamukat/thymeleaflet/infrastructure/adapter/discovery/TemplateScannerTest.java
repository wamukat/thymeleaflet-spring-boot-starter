package io.github.wamukat.thymeleaflet.infrastructure.adapter.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResolvedStorybookConfig;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookProperties;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

class TemplateScannerTest {

    @Test
    void scanTemplates_shouldReadConfiguredClasspathResourcesAndNormalizeTemplatePath() throws IOException {
        TemplateScanner scanner = new TemplateScanner(
            resolvedConfigWithTemplatePath("/templates/"),
            new StubResourcePatternResolver(new UriBackedResource(
                "<div th:fragment=\"sample\">Sample</div>",
                "file:/workspace/target/test-classes/templates/components/sample.html"
            ))
        );

        List<TemplateScanner.TemplateResource> templates = scanner.scanTemplates();

        assertThat(templates).hasSize(1);
        TemplateScanner.TemplateResource template = templates.getFirst();
        assertThat(template.templatePath()).isEqualTo("components/sample");
        assertThat(template.content()).contains("th:fragment=\"sample\"");
        assertThat(template.uri()).contains("templates/components/sample.html");
    }

    private static ResolvedStorybookConfig resolvedConfigWithTemplatePath(String templatePath) {
        StorybookProperties properties = new StorybookProperties();
        StorybookProperties.ResourceConfig resources = new StorybookProperties.ResourceConfig();
        resources.setTemplatePaths(List.of(templatePath));
        properties.setResources(resources);
        return ResolvedStorybookConfig.from(properties);
    }

    private static final class StubResourcePatternResolver implements ResourcePatternResolver {
        private final Resource resource;

        private StubResourcePatternResolver(Resource resource) {
            this.resource = resource;
        }

        @Override
        public Resource[] getResources(String locationPattern) {
            assertThat(locationPattern).isEqualTo("classpath:/templates/**/*.html");
            return new Resource[] {resource};
        }

        @Override
        public Resource getResource(String location) {
            return resource;
        }

        @Override
        public @Nullable ClassLoader getClassLoader() {
            return TemplateScannerTest.class.getClassLoader();
        }
    }

    private static final class UriBackedResource extends ByteArrayResource {
        private final String uri;

        private UriBackedResource(String content, String uri) {
            super(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            this.uri = uri;
        }

        @Override
        public URI getURI() {
            return URI.create(uri);
        }
    }
}
