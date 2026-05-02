package io.github.wamukat.thymeleaflet.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wamukat.thymeleaflet.testsupport.FixtureResources;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class HtmlParserAdapterComparisonTest {

    private final StructuredTemplateParser structuredParser = new StructuredTemplateParser();
    private final JsoupTemplateParserAdapter jsoupAdapter = new JsoupTemplateParserAdapter();

    @Test
    void jsoupAdapter_shouldPreserveThymeleafAttributesAcrossRegressionCorpus() {
        String html = FixtureResources.text("templates/regression/parser-corpus.html");

        StructuredTemplateParser.ParsedTemplate current = structuredParser.parse(html);
        CandidateTemplate candidate = jsoupAdapter.parse(html);

        assertThat(candidate.thymeleafAttributes())
            .containsAll(currentThymeleafAttributes(current));
        assertThat(candidate.thymeleafAttributes())
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
        String html = FixtureResources.text("templates/regression/parser-corpus.html");

        StructuredTemplateParser.ParsedTemplate current = structuredParser.parse(html);
        CandidateTemplate candidate = jsoupAdapter.parse(html);

        assertThat(candidate.fragmentDefinitions())
            .containsSequence(currentFragmentDefinitions(current));
    }

    @Test
    void jsoupAdapter_shouldTolerateMalformedHtmlButNotReplaceCurrentParserCapabilities() {
        String html = FixtureResources.text("templates/regression/parser-corpus.html");

        StructuredTemplateParser.ParsedTemplate current = structuredParser.parse(html);
        CandidateTemplate candidate = jsoupAdapter.parse(html);

        assertThat(currentElement(current, "malformedHtmlShell").attributeValue("th:fragment"))
            .hasValue("malformedHtmlShell");
        assertThat(candidate.element("malformedHtmlShell").flatMap(element -> element.attributeValue("th:fragment")))
            .hasValue("malformedHtmlShell");
        assertThat(candidate.thymeleafAttributes())
            .contains("data-th-text=${view.malformed.label}");

        assertThat(current.elements())
            .allSatisfy(element -> assertThat(element.line()).isPositive());
        assertThat(candidate.hasSourcePositions()).isFalse();
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

    private static List<String> currentThymeleafAttributes(StructuredTemplateParser.ParsedTemplate parsed) {
        return parsed.elements().stream()
            .flatMap(element -> element.thymeleafAttributes().stream())
            .map(attribute -> attribute.name() + "=" + attribute.value())
            .toList();
    }

    private static List<String> currentFragmentDefinitions(StructuredTemplateParser.ParsedTemplate parsed) {
        return parsed.elements().stream()
            .flatMap(element -> element.attributeValue("th:fragment").stream())
            .toList();
    }

    private static StructuredTemplateParser.TemplateElement currentElement(
        StructuredTemplateParser.ParsedTemplate parsed,
        String fragmentName
    ) {
        return parsed.elements().stream()
            .filter(element -> element.attributeValue("th:fragment").filter(fragmentName::equals).isPresent())
            .findFirst()
            .orElseThrow();
    }

    private static List<String> attributeValues(List<CandidateElement> elements, String attributeName) {
        return elements.stream()
            .flatMap(element -> element.attributeValue(attributeName).stream())
            .toList();
    }

    private static final class JsoupTemplateParserAdapter {

        CandidateTemplate parse(String html) {
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

    private record CandidateTemplate(List<CandidateElement> elements) {
        CandidateTemplate {
            elements = List.copyOf(elements);
        }

        List<String> thymeleafAttributes() {
            return elements.stream()
                .flatMap(element -> element.thymeleafAttributes().stream())
                .toList();
        }

        List<String> fragmentDefinitions() {
            return elements.stream()
                .flatMap(element -> element.attributeValue("th:fragment").stream())
                .toList();
        }

        Optional<CandidateElement> element(String fragmentName) {
            return elements.stream()
                .filter(element -> element.attributeValue("th:fragment").filter(fragmentName::equals).isPresent())
                .findFirst();
        }

        List<CandidateElement> subtree(CandidateElement root) {
            Map<Integer, CandidateElement> byIndex = elements.stream()
                .collect(Collectors.toUnmodifiableMap(CandidateElement::index, element -> element));
            Predicate<CandidateElement> isRootOrDescendant =
                element -> element.index() == root.index() || isDescendantOf(element, root.index(), byIndex);
            return elements.stream()
                .filter(isRootOrDescendant)
                .toList();
        }

        boolean hasSourcePositions() {
            return false;
        }

        private static boolean isDescendantOf(
            CandidateElement candidate,
            int rootIndex,
            Map<Integer, CandidateElement> byIndex
        ) {
            int parentIndex = candidate.parentIndex();
            while (parentIndex >= 0) {
                if (parentIndex == rootIndex) {
                    return true;
                }
                CandidateElement parent = byIndex.get(parentIndex);
                if (parent == null) {
                    return false;
                }
                parentIndex = parent.parentIndex();
            }
            return false;
        }
    }

    private record CandidateElement(
        String name,
        Map<String, String> attributes,
        int index,
        int parentIndex,
        int depth
    ) {
        CandidateElement {
            name = name.trim();
            attributes = Map.copyOf(attributes);
        }

        Optional<String> attributeValue(String attributeName) {
            Objects.requireNonNull(attributeName, "attributeName cannot be null");
            return Optional.ofNullable(attributes.get(attributeName));
        }

        List<String> thymeleafAttributes() {
            return attributes.entrySet().stream()
                .filter(entry -> isThymeleafAttribute(entry.getKey()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .toList();
        }

        private static boolean isThymeleafAttribute(String name) {
            String normalized = name.toLowerCase(Locale.ROOT);
            return normalized.startsWith("th:") || normalized.startsWith("data-th-");
        }
    }
}
