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
              <th:block th:replace="${dynamicRef}"></th:block>
            </div>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.referencedTemplatePaths())
            .contains("fragments/points-panel", "components/ui-alert")
            .doesNotContain("dynamicRef");
    }
}
