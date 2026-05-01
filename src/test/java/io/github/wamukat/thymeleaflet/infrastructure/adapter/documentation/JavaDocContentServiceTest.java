package io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation;

import io.github.wamukat.thymeleaflet.infrastructure.cache.ThymeleafletCacheManager;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResolvedStorybookConfig;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResourcePathValidator;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JavaDocContentServiceTest {

    @Test
    void loadJavaDocInfos_shouldRereadTemplateWhenCacheIsDisabled() throws Exception {
        Path template = new ClassPathResource("templates/cache/devtools-cache-sample.html").getFile().toPath();
        String original = Files.readString(template, StandardCharsets.UTF_8);
        JavaDocContentService service = buildService(false);

        try {
            Files.writeString(template, templateContent("Before update"), StandardCharsets.UTF_8);
            List<JavaDocAnalyzer.JavaDocInfo> before = service.loadJavaDocInfos("cache/devtools-cache-sample");

            Files.writeString(template, templateContent("After update"), StandardCharsets.UTF_8);
            List<JavaDocAnalyzer.JavaDocInfo> after = service.loadJavaDocInfos("cache/devtools-cache-sample");

            assertThat(before).singleElement()
                .extracting(JavaDocAnalyzer.JavaDocInfo::getDescription)
                .isEqualTo("Before update");
            assertThat(after).singleElement()
                .extracting(JavaDocAnalyzer.JavaDocInfo::getDescription)
                .isEqualTo("After update");
        } finally {
            Files.writeString(template, original, StandardCharsets.UTF_8);
        }
    }

    @Test
    void loadJavaDocInfos_shouldReuseTemplateWhenCacheIsEnabled() throws Exception {
        Path template = new ClassPathResource("templates/cache/devtools-cache-sample.html").getFile().toPath();
        String original = Files.readString(template, StandardCharsets.UTF_8);
        JavaDocContentService service = buildService(true);

        try {
            Files.writeString(template, templateContent("Cached value"), StandardCharsets.UTF_8);
            List<JavaDocAnalyzer.JavaDocInfo> before = service.loadJavaDocInfos("cache/devtools-cache-sample");

            Files.writeString(template, templateContent("Updated value"), StandardCharsets.UTF_8);
            List<JavaDocAnalyzer.JavaDocInfo> after = service.loadJavaDocInfos("cache/devtools-cache-sample");

            assertThat(before).singleElement()
                .extracting(JavaDocAnalyzer.JavaDocInfo::getDescription)
                .isEqualTo("Cached value");
            assertThat(after).singleElement()
                .extracting(JavaDocAnalyzer.JavaDocInfo::getDescription)
                .isEqualTo("Cached value");
        } finally {
            Files.writeString(template, original, StandardCharsets.UTF_8);
        }
    }

    private JavaDocContentService buildService(boolean cacheEnabled) {
        StorybookProperties properties = new StorybookProperties();
        StorybookProperties.CacheConfig cache = new StorybookProperties.CacheConfig();
        cache.setEnabled(cacheEnabled);
        properties.setCache(cache);

        ResolvedStorybookConfig resolved = ResolvedStorybookConfig.from(properties);
        return new JavaDocContentService(
            new JavaDocAnalyzer(),
            resolved,
            new ResourcePathValidator(),
            new ThymeleafletCacheManager(resolved)
        );
    }

    private String templateContent(String description) {
        return """
            <!--
            /**
             * %s
             * @example <div th:replace="~{cache/devtools-cache-sample :: sample()}"></div>
             */
            -->
            <div th:fragment="sample()">Sample</div>
            """.formatted(description);
    }
}
