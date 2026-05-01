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

    @Test
    void shouldIgnoreThymeleafAttributesInsideComments() {
        String html = """
            <section>
              <!-- <span th:text="${commented.out}"></span> -->
              <span th:text="${visible.value}"></span>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.modelPaths())
            .contains(ModelPath.of(List.of("visible", "value")))
            .doesNotContain(ModelPath.of(List.of("commented", "out")));
    }

    @Test
    void shouldHandleStructuredAttributesWithMultilineValuesAndQuotedSeparators() {
        String html = """
            <section
                th:with="localText='a > b',
                         otherText='still local'"
                th:each='item : ${view.items}'>
              <span th:text="${item.label + ' > ' + view.title}"></span>
              <th:block
                  data-th-replace="~{'components/complex-card' :: card(label=${view.title}, flag=true)}">
              </th:block>
              <span th:text="${localText + otherText}"></span>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.modelPaths())
            .contains(ModelPath.of(List.of("view", "items")))
            .contains(ModelPath.of(List.of("item", "label")))
            .contains(ModelPath.of(List.of("view", "title")))
            .doesNotContain(ModelPath.of(List.of("localText")))
            .doesNotContain(ModelPath.of(List.of("otherText")));
        assertThat(snapshot.loopVariablePaths()).containsEntry("item", ModelPath.of(List.of("view", "items")));
        assertThat(snapshot.referencedTemplatePathsWithRecursionFlags())
            .containsEntry("components/complex-card", true);
    }

    @Test
    void shouldExtractModelPathsFromUtilityFunctionArgumentsWithoutStaticClassNoise() {
        String html = """
            <section>
              <time th:text="${#temporals.format(item.publishedAt, 'yyyy-MM-dd')}">date</time>
              <span th:if="${T(java.time.LocalDate).now().isAfter(view.cutoffDate)}">future</span>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.modelPaths())
            .contains(ModelPath.of(List.of("item", "publishedAt")))
            .contains(ModelPath.of(List.of("view", "cutoffDate")))
            .doesNotContain(ModelPath.of(List.of("temporals")))
            .doesNotContain(ModelPath.of(List.of("java", "time", "LocalDate")));
    }

    @Test
    void shouldPreserveNoArgAndAliasBehaviorWithTokenizer() {
        String html = """
            <section th:with="current=${view.currentUser}">
              <article th:each="item : ${view.items}">
                <span th:if="${item.visible() and current.active()}"></span>
                <span th:text="${item.label ?: view.fallbackLabel}"></span>
              </article>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.loopVariablePaths()).containsEntry("item", ModelPath.of(List.of("view", "items")));
        assertThat(snapshot.modelPaths())
            .contains(ModelPath.of(List.of("view", "items")))
            .contains(ModelPath.of(List.of("item", "label")))
            .contains(ModelPath.of(List.of("view", "fallbackLabel")))
            .doesNotContain(ModelPath.of(List.of("current")))
            .doesNotContain(ModelPath.of(List.of("and")));
        assertThat(snapshot.noArgMethodPaths())
            .contains(ModelPath.of(List.of("item", "visible")))
            .doesNotContain(ModelPath.of(List.of("current", "active")));
    }

    @Test
    void shouldFailClosedForUnsupportedBracketAccessors() {
        String html = """
            <section>
              <span th:text="${view.map[key].label}"></span>
              <span th:text="${view.items[0].name}"></span>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.modelPaths())
            .contains(ModelPath.of(List.of("view", "map")))
            .contains(ModelPath.of(List.of("view", "items")))
            .doesNotContain(ModelPath.of(List.of("key")))
            .doesNotContain(ModelPath.of(List.of("view", "map", "label")))
            .doesNotContain(ModelPath.of(List.of("view", "items", "name")));
    }
}
