package io.github.wamukat.thymeleaflet.domain.service;

import io.github.wamukat.thymeleaflet.domain.model.ModelPath;
import io.github.wamukat.thymeleaflet.domain.model.TemplateInference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateModelExpressionAnalyzerTest {

    private final TemplateModelExpressionAnalyzer analyzer = new TemplateModelExpressionAnalyzer();

    @Test
    void shouldExtractModelPathsWithLoopAliasAndIgnoreThWithLocal() {
        String html = """
            <section th:with="localText='hello'">
              <p th:text="${pointPage.totalPoints}"></p>
              <article th:each="row : ${pointPage.items}">
                <p th:text="${row.amount}"></p>
              </article>
              <p th:text="${localText}"></p>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.modelPaths()).contains(ModelPath.of(List.of("pointPage", "totalPoints")));
        assertThat(snapshot.modelPaths()).contains(ModelPath.of(List.of("row", "amount")));
        assertThat(snapshot.modelPaths()).doesNotContain(ModelPath.of(List.of("localText")));
        assertThat(snapshot.loopVariablePaths()).containsEntry("row", ModelPath.of(List.of("pointPage", "items")));
    }

    @Test
    void shouldExtractStaticReferencedTemplatePaths() {
        String html = """
            <div>
              <th:block th:replace="~{fragments/points-panel :: pointsPanel}"></th:block>
              <th:block th:insert="~{components/ui-alert :: error('x')}"></th:block>
              <th:block th:replace="~{components/button :: primaryButton(label=${ctaLabel}, variant='primary')}"></th:block>
              <th:block th:replace="${dynamicRef}"></th:block>
            </div>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.referencedTemplatePaths())
            .contains("fragments/points-panel", "components/ui-alert")
            .doesNotContain("dynamicRef");

        assertThat(snapshot.referencedTemplatePathsWithRecursionFlags())
            .containsEntry("fragments/points-panel", true)
            .containsEntry("components/ui-alert", false)
            .containsEntry("components/button", true);
    }

    @Test
    void shouldExtractQuotedStaticReferencedTemplatePaths() {
        String html = """
            <div>
              <th:block th:replace="~{'fragments/quoted-panel' :: panel(label=${ctaLabel})}"></th:block>
              <th:block th:insert='~{"components/double-quoted" :: content}'></th:block>
            </div>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.referencedTemplatePaths())
            .contains("fragments/quoted-panel", "components/double-quoted");
        assertThat(snapshot.referencedTemplatePathsWithRecursionFlags())
            .containsEntry("fragments/quoted-panel", true)
            .containsEntry("components/double-quoted", true);
    }

    @Test
    void shouldExtractModelPathsAndReferencesFromDataThAttributes() {
        String html = """
            <section data-th-with="localText='hello'">
              <article data-th-each="item : ${view.items}">
                <span data-th-text="${item.label}">Label</span>
              </article>
              <th:block data-th-replace="~{components/data-card :: card(label=${view.title})}"></th:block>
              <th:block data-th-insert='~{"components/static-card" :: card}'></th:block>
              <span data-th-text="${localText}">Local</span>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.modelPaths())
            .contains(ModelPath.of(List.of("view", "items")))
            .contains(ModelPath.of(List.of("item", "label")))
            .contains(ModelPath.of(List.of("view", "title")))
            .doesNotContain(ModelPath.of(List.of("localText")));
        assertThat(snapshot.loopVariablePaths()).containsEntry("item", ModelPath.of(List.of("view", "items")));
        assertThat(snapshot.referencedTemplatePaths())
            .contains("components/data-card", "components/static-card");
    }

    @Test
    void shouldExtractNoArgMethodCallsSeparatelyFromModelPaths() {
        String html = """
            <section>
              <span th:if="${view.pointPage.hasPrev()}">Prev</span>
              <span th:text="${view.pointPage.nextPage()}"></span>
              <span th:text="${view.pointPage.format('yyyy-MM-dd')}"></span>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.modelPaths())
            .contains(ModelPath.of(List.of("view", "pointPage")))
            .doesNotContain(ModelPath.of(List.of("view", "pointPage", "hasPrev")))
            .doesNotContain(ModelPath.of(List.of("view", "pointPage", "nextPage")))
            .doesNotContain(ModelPath.of(List.of("view", "pointPage", "format")));

        assertThat(snapshot.noArgMethodPaths())
            .contains(ModelPath.of(List.of("view", "pointPage", "hasPrev")))
            .contains(ModelPath.of(List.of("view", "pointPage", "nextPage")))
            .doesNotContain(ModelPath.of(List.of("view", "pointPage", "format")));
    }
}
