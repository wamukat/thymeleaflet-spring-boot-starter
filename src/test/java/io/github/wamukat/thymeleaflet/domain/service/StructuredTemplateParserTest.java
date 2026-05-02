package io.github.wamukat.thymeleaflet.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wamukat.thymeleaflet.testsupport.FixtureResources;
import org.junit.jupiter.api.Test;

class StructuredTemplateParserTest {

    private final StructuredTemplateParser parser = new StructuredTemplateParser();

    @Test
    void parse_shouldExposeThymeleafAttributesWithOriginalNamesAndValues() {
        String html = """
            <section th:fragment="card(title)"
                     data-th-replace='~{components/button :: primary(label=${title})}'
                     th:with="
                       variant='primary',
                       enabled=${view.enabled}
                     ">
              <span th:text="${title}">Title</span>
            </section>
            """;

        StructuredTemplateParser.ParsedTemplate parsed = parser.parse(html);

        assertThat(parsed.elements())
            .extracting(StructuredTemplateParser.TemplateElement::name)
            .containsExactly("section", "span");

        StructuredTemplateParser.TemplateElement section = parsed.elements().getFirst();
        assertThat(section.attributeValue("th:fragment")).hasValue("card(title)");
        assertThat(section.attributeValue("data-th-replace"))
            .hasValue("~{components/button :: primary(label=${title})}");
        assertThat(section.attributeValue("th:with").orElseThrow())
            .contains("variant='primary'")
            .contains("enabled=${view.enabled}");

        assertThat(section.thymeleafAttributes())
            .extracting(StructuredTemplateParser.TemplateAttribute::name)
            .containsExactly("th:fragment", "data-th-replace", "th:with");
    }

    @Test
    void parse_shouldPreserveCommentsSeparatelyFromElements() {
        String html = """
            <!--
             /**
              * Card JavaDoc.
              * @fragment card
              */
            -->
            <div th:fragment="card">Card</div>
            """;

        StructuredTemplateParser.ParsedTemplate parsed = parser.parse(html);

        assertThat(parsed.comments()).hasSize(1);
        assertThat(parsed.comments().getFirst().content())
            .contains("@fragment card")
            .contains("Card JavaDoc.");
        assertThat(parsed.elements())
            .extracting(StructuredTemplateParser.TemplateElement::name)
            .containsExactly("div");
    }

    @Test
    void parse_shouldHandleNestedAndStandaloneElementsInDocumentOrder() {
        String html = """
            <main>
              <input data-th-value="${view.query}" />
              <article th:each="item : ${view.items}">
                <time th:text="${item.publishedAt}">date</time>
              </article>
            </main>
            """;

        StructuredTemplateParser.ParsedTemplate parsed = parser.parse(html);

        assertThat(parsed.elements())
            .extracting(StructuredTemplateParser.TemplateElement::name)
            .containsExactly("main", "input", "article", "time");
        assertThat(parsed.elements().get(1).attributeValue("data-th-value"))
            .hasValue("${view.query}");
        assertThat(parsed.elements().get(2).attributeValue("th:each"))
            .hasValue("item : ${view.items}");
    }

    @Test
    void parse_shouldExposeSubtreeForNestedElementsWithoutIncludingLaterSiblings() {
        String html = """
            <main>
              <section th:fragment="card">
                <section>
                  <span th:text="${view.title}">Title</span>
                </section>
              </section>
              <section th:fragment="other">
                <span th:text="${view.other}">Other</span>
              </section>
            </main>
            """;

        StructuredTemplateParser.ParsedTemplate parsed = parser.parse(html);
        StructuredTemplateParser.TemplateElement card = parsed.elements().stream()
            .filter(element -> element.attributeValue("th:fragment").filter("card"::equals).isPresent())
            .findFirst()
            .orElseThrow();

        assertThat(parsed.subtree(card))
            .extracting(StructuredTemplateParser.TemplateElement::name)
            .containsExactly("section", "section", "span");
    }

    @Test
    void parse_shouldExposeTextNodesWithoutTreatingCommentsAsText() {
        String html = """
            <section>
              <!-- ${commented.out} -->
              Hello ${view.title}
            </section>
            """;

        StructuredTemplateParser.ParsedTemplate parsed = parser.parse(html);

        assertThat(parsed.textNodes())
            .extracting(StructuredTemplateParser.TemplateText::content)
            .anySatisfy(text -> assertThat(text).contains("Hello ${view.title}"))
            .noneSatisfy(text -> assertThat(text).contains("commented.out"));
        assertThat(parsed.comments()).singleElement()
            .extracting(StructuredTemplateParser.TemplateComment::content)
            .asString()
            .contains("commented.out");
    }

    @Test
    void parse_shouldCoverRealRegressionCorpusFixtures() {
        String html = FixtureResources.text("templates/regression/parser-corpus.html");

        StructuredTemplateParser.ParsedTemplate parsed = parser.parse(html);

        assertThat(fragmentDefinitions(parsed))
            .contains(
                "dataThList",
                "quotedSelectorShell",
                "quotedTarget(label)",
                "noArgReferenceShell",
                "noArgReferenceTarget",
                "nestedFragmentShell",
                "nestedChild(title)",
                "malformedHtmlShell",
                "mixedDependencyShell",
                "siblingBoundaryA",
                "siblingBoundaryB"
            );
        assertThat(parsed.elements())
            .anySatisfy(element -> assertThat(element.attributeValue("data-th-each"))
                .hasValue("item : ${view.items}"))
            .anySatisfy(element -> assertThat(element.attributeValue("th:replace"))
                .hasValue("~{'regression/parser-corpus' :: quotedTarget(label=${view.label})}"))
            .anySatisfy(element -> assertThat(element.attributeValue("th:replace"))
                .hasValue("~{regression/parser-corpus :: noArgReferenceTarget()}"))
            .anySatisfy(element -> assertThat(element.attributeValue("data-th-text"))
                .hasValue("${view.malformed.label}"))
            .anySatisfy(element -> assertThat(element.attributeValue("th:include"))
                .hasValue("~{regression/parser-corpus :: nestedChild(title='Included > Nested')}"))
            .anySatisfy(element -> assertThat(element.attributeValue("data-th-include"))
                .hasValue("~{regression/parser-corpus :: nestedChild(title=${view.mixed.title})}"))
            .anySatisfy(element -> assertThat(element.attributeValue("data-th-value"))
                .hasValue("${view.mixed.title}"));
        assertThat(parsed.comments())
            .extracting(StructuredTemplateParser.TemplateComment::content)
            .anySatisfy(comment -> assertThat(comment).contains("GH-149-style"))
            .anySatisfy(comment -> assertThat(comment).contains("malformed-but-browser-tolerated HTML"))
            .anySatisfy(comment -> assertThat(comment).contains("commented.out"));
        assertThat(parsed.textNodes())
            .extracting(StructuredTemplateParser.TemplateText::content)
            .anySatisfy(text -> assertThat(text).contains("Hello ${view.mixed.title}"))
            .noneSatisfy(text -> assertThat(text).contains("commented.out"));
    }

    @Test
    void parse_shouldExposeExpectedSubtreesForRegressionCorpusSiblings() {
        String html = FixtureResources.text("templates/regression/parser-corpus.html");

        StructuredTemplateParser.ParsedTemplate parsed = parser.parse(html);

        assertThat(attributeValues(parsed.subtree(fragmentElement(parsed, "nestedFragmentShell")), "th:fragment"))
            .contains("nestedFragmentShell", "nestedChild(title)");
        assertThat(attributeValues(parsed.subtree(fragmentElement(parsed, "siblingBoundaryA")), "th:text"))
            .contains("${view.boundary.a}")
            .doesNotContain("${view.boundary.b}");
        assertThat(attributeValues(parsed.subtree(fragmentElement(parsed, "siblingBoundaryB")), "th:text"))
            .contains("${view.boundary.b}")
            .doesNotContain("${view.boundary.a}");
        assertThat(parsed.subtree(fragmentElement(parsed, "mixedDependencyShell")))
            .flatExtracting(StructuredTemplateParser.TemplateElement::thymeleafAttributes)
            .extracting(StructuredTemplateParser.TemplateAttribute::name)
            .contains(
                "th:replace",
                "th:insert",
                "th:include",
                "data-th-replace",
                "data-th-insert",
                "data-th-include",
                "data-th-value"
            );
    }

    @Test
    void parseWithDiagnostics_shouldWarnForDynamicFragmentReferences() {
        String html = """
            <section>
              <div th:replace="${dynamicReference}"></div>
              <div data-th-insert="~{components/card :: card()}"></div>
            </section>
            """;

        StructuredTemplateParser.TemplateParseResult result = parser.parseWithDiagnostics(html);

        assertThat(result.parsedTemplate().elements())
            .extracting(StructuredTemplateParser.TemplateElement::name)
            .containsExactly("section", "div", "div");
        assertThat(result.diagnostics()).singleElement()
            .satisfies(diagnostic -> {
                assertThat(diagnostic.code()).isEqualTo("TEMPLATE_DYNAMIC_FRAGMENT_REFERENCE_SKIPPED");
                assertThat(diagnostic.message()).contains("th:replace");
                assertThat(diagnostic.line()).isGreaterThan(0);
                assertThat(diagnostic.column()).isGreaterThan(0);
            });
    }

    @Test
    void parseWithDiagnostics_shouldReturnNoWarningsForStaticMarkup() {
        StructuredTemplateParser.TemplateParseResult result = parser.parseWithDiagnostics("""
            <section th:replace="~{components/card :: card()}">Card</section>
            """);

        assertThat(result.parsedTemplate().elements()).hasSize(1);
        assertThat(result.diagnostics()).isEmpty();
    }

    private static java.util.List<String> fragmentDefinitions(StructuredTemplateParser.ParsedTemplate parsed) {
        return parsed.elements().stream()
            .flatMap(element -> element.attributeValue("th:fragment").stream())
            .toList();
    }

    private static StructuredTemplateParser.TemplateElement fragmentElement(
        StructuredTemplateParser.ParsedTemplate parsed,
        String fragmentName
    ) {
        return parsed.elements().stream()
            .filter(element -> element.attributeValue("th:fragment").filter(fragmentName::equals).isPresent())
            .findFirst()
            .orElseThrow();
    }

    private static java.util.List<String> attributeValues(
        java.util.List<StructuredTemplateParser.TemplateElement> elements,
        String attributeName
    ) {
        return elements.stream()
            .flatMap(element -> element.attributeValue(attributeName).stream())
            .toList();
    }
}
