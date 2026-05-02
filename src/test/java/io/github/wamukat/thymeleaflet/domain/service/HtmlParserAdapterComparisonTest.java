package io.github.wamukat.thymeleaflet.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wamukat.thymeleaflet.testsupport.FixtureResources;
import io.github.wamukat.thymeleaflet.testsupport.parser.CandidateElement;
import io.github.wamukat.thymeleaflet.testsupport.parser.CandidateHtmlParserAdapter;
import io.github.wamukat.thymeleaflet.testsupport.parser.CandidateTemplate;
import io.github.wamukat.thymeleaflet.testsupport.parser.ParserComparisonContract;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

class HtmlParserAdapterComparisonTest {

    private final StructuredTemplateParser structuredParser = new StructuredTemplateParser();
    private final JsoupTemplateParserAdapter jsoupAdapter = new JsoupTemplateParserAdapter();

    @Test
    void jsoupAdapter_shouldPreserveThymeleafAttributesAcrossRegressionCorpus() {
        ParserComparisonContract.Comparison comparison = comparison();

        assertThat(comparison.candidate().thymeleafAttributes())
            .containsAll(comparison.currentThymeleafAttributes());
        assertThat(comparison.candidate().thymeleafAttributes())
            .contains(
                "data-th-each=item : ${view.items}",
                "th:replace=~{'regression/parser-corpus' :: quotedTarget(label=${view.label})}",
                "data-th-text=${view.malformed.label}",
                "th:include=~{regression/parser-corpus :: nestedChild(title='Included > Nested')}",
                "data-th-include=~{regression/parser-corpus :: nestedChild(title=${view.mixed.title})}",
                "data-th-value=${view.mixed.title}"
            );
    }

    @Test
    void jsoupAdapter_shouldKeepFragmentDeclarationOrderStableForCorpus() {
        ParserComparisonContract.Comparison comparison = comparison();

        assertThat(comparison.candidate().fragmentDefinitions())
            .containsSequence(comparison.currentFragmentDefinitions());
    }

    @Test
    void jsoupAdapter_shouldTolerateMalformedHtmlButNotReplaceCurrentParserCapabilities() {
        ParserComparisonContract.Comparison comparison = comparison();

        assertThat(comparison.currentElement("malformedHtmlShell").attributeValue("th:fragment"))
            .hasValue("malformedHtmlShell");
        assertThat(comparison.candidate().element("malformedHtmlShell").flatMap(element -> element.attributeValue("th:fragment")))
            .hasValue("malformedHtmlShell");
        assertThat(comparison.candidate().thymeleafAttributes())
            .contains("data-th-text=${view.malformed.label}");

        assertThat(comparison.current().elements())
            .allSatisfy(element -> assertThat(element.line()).isPositive());
        assertThat(comparison.candidate().hasSourcePositions()).isFalse();
    }

    @Test
    void jsoupAdapter_shouldRespectSiblingSubtreeBoundariesForCandidateEvaluation() {
        String html = FixtureResources.text("templates/regression/parser-corpus.html");

        CandidateTemplate candidate = jsoupAdapter.parse(html);

        assertThat(attributeValues(candidate.subtree(candidate.element("siblingBoundaryA").orElseThrow()), "th:text"))
            .contains("${view.boundary.a}")
            .doesNotContain("${view.boundary.b}");
        assertThat(attributeValues(candidate.subtree(candidate.element("siblingBoundaryB").orElseThrow()), "th:text"))
            .contains("${view.boundary.b}")
            .doesNotContain("${view.boundary.a}");
    }

    private ParserComparisonContract.Comparison comparison() {
        return new ParserComparisonContract(structuredParser).compare(
            FixtureResources.text("templates/regression/parser-corpus.html"),
            jsoupAdapter
        );
    }

    private static List<String> attributeValues(List<CandidateElement> elements, String attributeName) {
        return elements.stream()
            .flatMap(element -> element.attributeValue(attributeName).stream())
            .toList();
    }

    private static final class JsoupTemplateParserAdapter implements CandidateHtmlParserAdapter {

        @Override
        public CandidateTemplate parse(String html) {
            Objects.requireNonNull(html, "html cannot be null");
            Element root = Jsoup.parse(html, "", Parser.htmlParser());
            List<CandidateElement> elements = new ArrayList<>();
            collect(root, -1, 0, elements);
            return new CandidateTemplate(elements);
        }

        private static void collect(Element element, int parentIndex, int depth, List<CandidateElement> elements) {
            int index = elements.size();
            Map<String, String> attributes = element.attributes().asList().stream()
                .collect(Collectors.toMap(
                    Attribute::getKey,
                    Attribute::getValue,
                    (first, second) -> first
                ));
            elements.add(new CandidateElement(element.normalName(), attributes, index, parentIndex, depth));
            for (Element child : element.children()) {
                collect(child, index, depth + 1, elements);
            }
        }
    }
}
