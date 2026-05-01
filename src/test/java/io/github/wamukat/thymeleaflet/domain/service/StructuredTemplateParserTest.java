package io.github.wamukat.thymeleaflet.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

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
}
