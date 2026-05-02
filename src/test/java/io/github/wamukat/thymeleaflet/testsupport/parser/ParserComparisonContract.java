package io.github.wamukat.thymeleaflet.testsupport.parser;

import io.github.wamukat.thymeleaflet.domain.service.StructuredTemplateParser;

import java.util.List;
import java.util.Objects;

public final class ParserComparisonContract {

    private final StructuredTemplateParser structuredParser;

    public ParserComparisonContract(StructuredTemplateParser structuredParser) {
        this.structuredParser = Objects.requireNonNull(structuredParser, "structuredParser cannot be null");
    }

    public Comparison compare(String html, CandidateHtmlParserAdapter candidateAdapter) {
        Objects.requireNonNull(html, "html cannot be null");
        Objects.requireNonNull(candidateAdapter, "candidateAdapter cannot be null");
        return new Comparison(structuredParser.parse(html), candidateAdapter.parse(html));
    }

    public record Comparison(
        StructuredTemplateParser.ParsedTemplate current,
        CandidateTemplate candidate
    ) {
        public List<String> currentThymeleafAttributes() {
            return current.elements().stream()
                .flatMap(element -> element.thymeleafAttributes().stream())
                .map(attribute -> attribute.name() + "=" + attribute.value())
                .toList();
        }

        public List<String> currentFragmentDefinitions() {
            return current.elements().stream()
                .flatMap(element -> element.attributeValue("th:fragment").stream())
                .toList();
        }

        public StructuredTemplateParser.TemplateElement currentElement(String fragmentName) {
            Objects.requireNonNull(fragmentName, "fragmentName cannot be null");
            return current.elements().stream()
                .filter(element -> element.attributeValue("th:fragment").filter(fragmentName::equals).isPresent())
                .findFirst()
                .orElseThrow();
        }
    }
}
