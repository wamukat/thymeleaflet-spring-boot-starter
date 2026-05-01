package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocAnalyzer;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocContentService;
import io.github.wamukat.thymeleaflet.infrastructure.cache.ThymeleafletCacheManager;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResolvedStorybookConfig;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResourcePathValidator;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookProperties;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JavaDocLookupServiceTest {

    @Test
    void findJavaDocInfo_shouldMatchExplicitFragmentTagForLocalizedDescription() {
        JavaDocAnalyzer.JavaDocInfo docInfo = JavaDocAnalyzer.JavaDocInfo.of(
            "会員情報パネル（HTMX パーシャル）",
            Collections.emptyList(),
            List.of(JavaDocAnalyzer.ModelInfo.required("field1", "String", "フィールド1")),
            Optional.of("myPanel"),
            Collections.emptyList(),
            Optional.empty()
        );
        JavaDocContentService javaDocContentService = new StubJavaDocContentService(List.of(docInfo));
        JavaDocLookupService service = new JavaDocLookupService(javaDocContentService);

        Optional<JavaDocAnalyzer.JavaDocInfo> result = service.findJavaDocInfo("partials/my-panel", "myPanel");

        assertThat(result).containsSame(docInfo);
    }

    @Test
    void findJavaDocInfo_shouldStillMatchExampleWhenFragmentTagIsAbsent() {
        JavaDocAnalyzer.JavaDocInfo docInfo = JavaDocAnalyzer.JavaDocInfo.of(
            "Localized panel description",
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(JavaDocAnalyzer.ExampleInfo.of("partials/my-panel", "myPanel")),
            Optional.empty()
        );
        JavaDocContentService javaDocContentService = new StubJavaDocContentService(List.of(docInfo));
        JavaDocLookupService service = new JavaDocLookupService(javaDocContentService);

        Optional<JavaDocAnalyzer.JavaDocInfo> result = service.findJavaDocInfo("partials/my-panel", "myPanel");

        assertThat(result).containsSame(docInfo);
    }

    private static final class StubJavaDocContentService extends JavaDocContentService {

        private final List<JavaDocAnalyzer.JavaDocInfo> docs;

        private StubJavaDocContentService(List<JavaDocAnalyzer.JavaDocInfo> docs) {
            this(docs, config());
        }

        private StubJavaDocContentService(List<JavaDocAnalyzer.JavaDocInfo> docs, ResolvedStorybookConfig config) {
            super(
                new JavaDocAnalyzer(),
                config,
                new ResourcePathValidator(),
                new ThymeleafletCacheManager(config)
            );
            this.docs = docs;
        }

        private static ResolvedStorybookConfig config() {
            return ResolvedStorybookConfig.from(new StorybookProperties());
        }

        @Override
        public List<JavaDocAnalyzer.JavaDocInfo> loadJavaDocInfos(String templatePath) {
            return docs;
        }
    }
}
