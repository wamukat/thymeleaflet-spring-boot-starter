package io.github.wamukat.thymeleaflet.domain.service;

import io.github.wamukat.thymeleaflet.domain.model.ModelPath;
import io.github.wamukat.thymeleaflet.domain.model.TemplateInference;
import io.github.wamukat.thymeleaflet.testsupport.FixtureResources;
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
    void shouldIgnoreLoopStatusPropertiesOnlyInsideLoopScope() {
        String html = """
            <section>
              <p th:text="${stat.summary}"></p>
              <p th:text="${stat.index}"></p>
              <article th:each="row, stat : ${pointPage.items}">
                <p th:text="${stat}"></p>
                <p th:text="${stat.index}"></p>
                <p th:text="${stat.customValue}"></p>
                <p th:text="${row.amount}"></p>
              </article>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.modelPaths())
            .contains(ModelPath.of(List.of("pointPage", "items")))
            .contains(ModelPath.of(List.of("stat", "summary")))
            .contains(ModelPath.of(List.of("stat", "index")))
            .contains(ModelPath.of(List.of("row", "amount")))
            .doesNotContain(ModelPath.of(List.of("stat")))
            .doesNotContain(ModelPath.of(List.of("stat", "customValue")))
            .filteredOn(path -> path.equals(ModelPath.of(List.of("stat", "index"))))
            .hasSize(1);
        assertThat(snapshot.loopVariablePaths())
            .containsEntry("row", ModelPath.of(List.of("pointPage", "items")))
            .doesNotContainKey("stat");
    }

    @Test
    void shouldIgnoreLoopStatusPropertiesInInlineTextOnlyInsideLoopScope() {
        String html = """
            <section>
              [[${stat.index}]]
              <article th:each="row, stat : ${pointPage.items}">
                [[${stat}]]
                [[${stat.index}]]
                [[${stat.customValue}]]
                [[${row.amount}]]
              </article>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.modelPaths())
            .contains(ModelPath.of(List.of("pointPage", "items")))
            .contains(ModelPath.of(List.of("stat", "index")))
            .contains(ModelPath.of(List.of("row", "amount")))
            .doesNotContain(ModelPath.of(List.of("stat")))
            .doesNotContain(ModelPath.of(List.of("stat", "customValue")))
            .filteredOn(path -> path.equals(ModelPath.of(List.of("stat", "index"))))
            .hasSize(1);
        assertThat(snapshot.loopVariablePaths())
            .containsEntry("row", ModelPath.of(List.of("pointPage", "items")))
            .doesNotContainKey("stat");
    }

    @Test
    void shouldNotApplyDeclaredLoopStatusAliasToOwnIterableExpression() {
        String html = """
            <section>
              <article th:each="row, stat : ${stat.items}">
                <p th:text="${stat}"></p>
                <p th:text="${stat.index}"></p>
                <p th:text="${row.amount}"></p>
              </article>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.modelPaths())
            .contains(ModelPath.of(List.of("stat", "items")))
            .contains(ModelPath.of(List.of("row", "amount")))
            .doesNotContain(ModelPath.of(List.of("stat")))
            .doesNotContain(ModelPath.of(List.of("stat", "index")));
        assertThat(snapshot.loopVariablePaths())
            .containsEntry("row", ModelPath.of(List.of("stat", "items")))
            .doesNotContainKey("stat");
    }

    @Test
    void shouldNotMapLoopAliasToAncestorLoopStatusIterablePath() {
        String html = """
            <section>
              <article th:each="group, stat : ${groups}">
                <p th:each="item : ${stat.current.items}" th:text="${item.name}"></p>
              </article>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.modelPaths())
            .contains(ModelPath.of(List.of("groups")))
            .contains(ModelPath.of(List.of("item", "name")))
            .doesNotContain(ModelPath.of(List.of("stat", "current", "items")));
        assertThat(snapshot.loopVariablePaths())
            .containsEntry("group", ModelPath.of(List.of("groups")))
            .doesNotContainKey("stat")
            .doesNotContainKey("item");
    }

    @Test
    void shouldIgnoreImplicitLoopStatusAliasInsideLoopScope() {
        String html = """
            <section>
              <p th:text="${rowStat.index}"></p>
              [[${rowStat.index}]]
              <article th:each="row : ${pointPage.items}">
                <p th:text="${rowStat.index}"></p>
                <p th:text="${rowStat.customValue}"></p>
                [[${rowStat.count}]]
                <p th:text="${row.amount}"></p>
              </article>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.modelPaths())
            .contains(ModelPath.of(List.of("pointPage", "items")))
            .contains(ModelPath.of(List.of("rowStat", "index")))
            .contains(ModelPath.of(List.of("row", "amount")))
            .doesNotContain(ModelPath.of(List.of("rowStat", "customValue")))
            .doesNotContain(ModelPath.of(List.of("rowStat", "count")))
            .filteredOn(path -> path.equals(ModelPath.of(List.of("rowStat", "index"))))
            .hasSize(2);
        assertThat(snapshot.loopVariablePaths())
            .containsEntry("row", ModelPath.of(List.of("pointPage", "items")))
            .doesNotContainKey("rowStat");
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
    void shouldResolveSameTemplateReferencedTemplatePathsWhenCurrentTemplatePathIsProvided() {
        String html = """
            <section>
              <th:block th:replace="~{:: header(title=${view.title})}"></th:block>
              <th:block th:insert="~{this :: footer()}"></th:block>
              <th:block th:replace="~{${dynamicPath} :: ignored()}"></th:block>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of(), "pages/dashboard");

        assertThat(snapshot.referencedTemplatePaths())
            .containsExactly("pages/dashboard");
        assertThat(snapshot.referencedTemplatePathsWithRecursionFlags())
            .containsEntry("pages/dashboard", true);
    }

    @Test
    void shouldExtractSelectorStyleReferencedTemplatePaths() {
        String html = """
            <section>
              <th:block th:replace="~{components/header :: #site-header}"></th:block>
              <th:block th:insert="~{components/card :: .summary-card(title=${view.title})}"></th:block>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.referencedTemplatePaths())
            .contains("components/header", "components/card");
        assertThat(snapshot.referencedTemplatePathsWithRecursionFlags())
            .containsEntry("components/header", true)
            .containsEntry("components/card", true);
    }

    @Test
    void shouldPreserveReferencedFragmentRawArguments() {
        String html = """
            <section>
              <th:block th:replace="~{components/card :: card(variant=${view.variant}, title='Ready')}"></th:block>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.referencedFragments()).singleElement()
            .satisfies(reference -> {
                assertThat(reference.templatePath()).isEqualTo("components/card");
                assertThat(reference.fragmentName()).isEqualTo("card");
                assertThat(reference.arguments()).containsExactly("variant=${view.variant}", "title='Ready'");
                assertThat(reference.hasArgumentList()).isTrue();
                assertThat(reference.requiresChildModelRecursion()).isTrue();
            });
    }

    @Test
    void shouldContinueSkippingSameTemplateReferencesWithoutCurrentTemplatePath() {
        String html = """
            <section>
              <th:block th:replace="~{:: header(title=${view.title})}"></th:block>
              <th:block th:insert="~{this :: footer()}"></th:block>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.referencedTemplatePaths()).isEmpty();
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
    void shouldExtractExpressionsWithLiteralBracesAndSelectionExpressions() {
        String html = """
            <section>
              <span th:text="${'}' + view.title}"></span>
              <span th:title="prefix ${view.subtitle} suffix ${view.count}"></span>
              <input data-th-value="*{selected.label}" />
              <span th:text="${view.unclosed"></span>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.modelPaths())
            .contains(ModelPath.of(List.of("view", "title")))
            .contains(ModelPath.of(List.of("view", "subtitle")))
            .contains(ModelPath.of(List.of("view", "count")))
            .contains(ModelPath.of(List.of("selected", "label")))
            .doesNotContain(ModelPath.of(List.of("view", "unclosed")));
    }

    @Test
    void shouldResolveSelectionExpressionsAgainstActiveThObject() {
        String html = """
            <form th:object="${profileForm}">
              <input th:field="*{email}" />
              <input th:field="*{enabled}" />
              <span th:text="*{address.city}"></span>
            </form>
            <input th:field="*{standalone.value}" />
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.modelPaths())
            .contains(ModelPath.of(List.of("profileForm")))
            .contains(ModelPath.of(List.of("profileForm", "email")))
            .contains(ModelPath.of(List.of("profileForm", "enabled")))
            .contains(ModelPath.of(List.of("profileForm", "address", "city")))
            .contains(ModelPath.of(List.of("standalone", "value")))
            .doesNotContain(ModelPath.of(List.of("email")))
            .doesNotContain(ModelPath.of(List.of("enabled")))
            .doesNotContain(ModelPath.of(List.of("address", "city")));
    }

    @Test
    void shouldNotInferSelectionFieldsWhenThObjectRootIsExcluded() {
        String html = """
            <form th:object="${profileForm}">
              <input th:field="*{email}" />
              <span th:text="*{address.city}"></span>
            </form>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of("profileForm"));

        assertThat(snapshot.modelPaths())
            .doesNotContain(ModelPath.of(List.of("profileForm")))
            .doesNotContain(ModelPath.of(List.of("profileForm", "email")))
            .doesNotContain(ModelPath.of(List.of("email")))
            .doesNotContain(ModelPath.of(List.of("address", "city")));
    }

    @Test
    void shouldResolveLoopIterableSelectionExpressionAgainstActiveThObject() {
        String html = """
            <form th:object="${orderForm}">
              <article th:each="item : *{items}">
                <p th:text="${item.name}"></p>
              </article>
            </form>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.modelPaths())
            .contains(ModelPath.of(List.of("orderForm")))
            .contains(ModelPath.of(List.of("item", "name")));
        assertThat(snapshot.loopVariablePaths())
            .containsEntry("item", ModelPath.of(List.of("orderForm", "items")));
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
    void shouldInferStableBracketAccessorsAndFailClosedForDynamicKeys() {
        String html = """
            <section>
              <span th:text="${view['display-name']}"></span>
              <span th:text="${view[display-name]}"></span>
              <span th:text="${view.map[key].label}"></span>
              <span th:text="${view.items[0].name}"></span>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.modelPaths())
            .contains(ModelPath.of(List.of("view", "display-name")))
            .contains(ModelPath.of(List.of("view", "map")))
            .contains(ModelPath.of(List.of("view", "items", "[]", "name")))
            .doesNotContain(ModelPath.of(List.of("key")))
            .doesNotContain(ModelPath.of(List.of("view", "map", "label")))
            .doesNotContain(ModelPath.of(List.of("view", "items", "name")));
    }

    @Test
    void shouldBuildListModelForStableIndexedBracketAccessors() {
        String html = """
            <section>
              <span th:text="${view.items[0].name}"></span>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.toInferredModel().toMap())
            .containsEntry("view", java.util.Map.of(
                "items", java.util.List.of(java.util.Map.of("name", "Sample name"))
            ));
    }

    @Test
    void shouldBuildNestedListModelForIndexedBracketAccessorsInsideLoopAliases() {
        String html = """
            <section>
              <article th:each="item : ${view.items}">
                <span th:text="${item.tags[0].label}"></span>
              </article>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.toInferredModel().toMap())
            .containsEntry("view", java.util.Map.of(
                "items", java.util.List.of(java.util.Map.of(
                    "tags", java.util.List.of(java.util.Map.of("label", "Sample label"))
                ))
            ));
    }

    @Test
    void shouldInferRepresentativeValuesFromSwitchCaseLiterals() {
        String html = """
            <section>
              <div th:switch="${view.status}">
                <span th:case="*">Default</span>
                <span th:case="'active'">Active</span>
                <span th:case="${dynamicCase}">Dynamic</span>
              </div>
              <div data-th-switch="${view.count}">
                <span data-th-case="3">Three</span>
              </div>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.modelPaths())
            .contains(ModelPath.of(List.of("view", "status")))
            .contains(ModelPath.of(List.of("view", "count")));
        assertThat(snapshot.representativeValues())
            .containsEntry(ModelPath.of(List.of("view", "status")), "active")
            .containsEntry(ModelPath.of(List.of("view", "count")), 3)
            .doesNotContainValue("*");
        assertThat(snapshot.toInferredModel().toMap())
            .containsEntry("view", java.util.Map.of("status", "active", "count", 3));
    }

    @Test
    void shouldInferSwitchCaseValuesInsideSelectionAndLoopScopes() {
        String html = """
            <form th:object="${profileForm}">
              <div th:switch="*{role}">
                <span th:case="'admin'">Admin</span>
              </div>
              <article th:each="item : *{items}">
                <div th:switch="${item.state}">
                  <span th:case="'ready'">Ready</span>
                </div>
              </article>
            </form>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.representativeValues())
            .containsEntry(ModelPath.of(List.of("profileForm", "role")), "admin")
            .containsEntry(ModelPath.of(List.of("item", "state")), "ready");
        assertThat(snapshot.toInferredModel().toMap())
            .containsEntry("profileForm", java.util.Map.of(
                "role", "admin",
                "items", java.util.List.of(java.util.Map.of("state", "ready"))
            ));
    }

    @Test
    void shouldNotUseNestedSwitchCaseAsOuterRepresentativeValue() {
        String html = """
            <section>
              <div th:switch="${view.status}">
                <section th:case="*">
                  <div th:switch="${view.cardType}">
                    <span th:case="'card'">Card</span>
                  </div>
                </section>
                <span th:case="'active'">Active</span>
              </div>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.representativeValues())
            .containsEntry(ModelPath.of(List.of("view", "status")), "active")
            .containsEntry(ModelPath.of(List.of("view", "cardType")), "card");
        assertThat(snapshot.toInferredModel().toMap())
            .containsEntry("view", java.util.Map.of("status", "active", "cardType", "card"));
    }

    @Test
    void shouldNotInferRepresentativeValueFromComputedSwitchExpression() {
        String html = """
            <section>
              <div th:switch="${view.status == 'active'}">
                <span th:case="true">Active</span>
                <span th:case="false">Inactive</span>
              </div>
              <div th:switch="${view.resolveStatus()}">
                <span th:case="'ready'">Ready</span>
              </div>
            </section>
            """;

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.modelPaths())
            .contains(ModelPath.of(List.of("view", "status")))
            .doesNotContain(ModelPath.of(List.of("view")));
        assertThat(snapshot.noArgMethodPaths())
            .contains(ModelPath.of(List.of("view", "resolveStatus")));
        assertThat(snapshot.representativeValues())
            .doesNotContainKey(ModelPath.of(List.of("view", "status")))
            .doesNotContainKey(ModelPath.of(List.of("view")));
    }

    @Test
    void shouldAnalyzeRealRegressionCorpusFixture() {
        String html = FixtureResources.text("templates/regression/parser-corpus.html");

        TemplateInference snapshot = analyzer.analyze(html, Set.of());

        assertThat(snapshot.modelPaths())
            .contains(ModelPath.of(List.of("view", "items")))
            .contains(ModelPath.of(List.of("item", "label")))
            .contains(ModelPath.of(List.of("view", "label")))
            .contains(ModelPath.of(List.of("view", "nested", "title")))
            .contains(ModelPath.of(List.of("view", "malformed", "label")));
        assertThat(snapshot.loopVariablePaths())
            .containsEntry("item", ModelPath.of(List.of("view", "items")));
        assertThat(snapshot.referencedTemplatePathsWithRecursionFlags())
            .containsEntry("regression/parser-corpus", true);
    }
}
