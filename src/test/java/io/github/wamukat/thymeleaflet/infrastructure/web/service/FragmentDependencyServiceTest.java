package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.infrastructure.cache.ThymeleafletCacheManager;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResolvedStorybookConfig;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.ResourcePathValidator;
import io.github.wamukat.thymeleaflet.infrastructure.configuration.StorybookProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FragmentDependencyServiceTest {

    @Mock
    private ResourcePathValidator resourcePathValidator;

    @Test
    void findDependencies_extractsDataThDependenciesInsideTargetFragmentOnly() {
        String html = """
            <main>
              <section data-th-fragment="card(title)">
                <section>
                  <span data-th-replace='~{components/title :: heading(text=${title})}'></span>
                </section>
                <input data-th-insert="~{components/field :: textField(value=${title})}" />
              </section>
              <section th:fragment="other">
                <span th:replace="~{components/other :: ignored()}"></span>
              </section>
            </main>
            """;
        FragmentDependencyService service = buildService("pages/card", html);

        List<FragmentDependencyService.DependencyComponent> dependencies =
            service.findDependencies("pages/card", "card");

        assertThat(dependencies)
            .extracting(FragmentDependencyService.DependencyComponent::key)
            .containsExactly(
                "components/title::heading",
                "components/field::textField"
            );
    }

    @Test
    void findDependencies_handlesQuotedTemplatePathsAndDeduplicatesDependencies() {
        String html = """
            <article th:fragment="panel">
              <div th:replace="~{'components/panel-header' :: header()}"></div>
              <div data-th-include="~{'components/panel-header' :: header()}"></div>
              <div th:insert="~{components/panel-body :: body()}"></div>
            </article>
            """;
        FragmentDependencyService service = buildService("pages/panel", html);

        List<FragmentDependencyService.DependencyComponent> dependencies =
            service.findDependencies("pages/panel", "panel");

        assertThat(dependencies)
            .extracting(FragmentDependencyService.DependencyComponent::key)
            .containsExactly(
                "components/panel-header::header",
                "components/panel-body::body"
            );
    }

    private FragmentDependencyService buildService(String templatePath, String html) {
        StorybookProperties properties = new StorybookProperties();
        ResolvedStorybookConfig config = ResolvedStorybookConfig.from(properties, false);
        ThymeleafletCacheManager cacheManager = new ThymeleafletCacheManager(config);
        when(resourcePathValidator.findTemplate(eq(templatePath), eq(config.getResources().getTemplatePaths())))
            .thenReturn(resource(html));
        return new FragmentDependencyService(config, resourcePathValidator, cacheManager);
    }

    private ByteArrayResource resource(String html) {
        return new ByteArrayResource(html.getBytes(StandardCharsets.UTF_8));
    }
}
