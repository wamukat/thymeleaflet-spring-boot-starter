package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.domain.service.FragmentReferenceAttributes;
import io.github.wamukat.thymeleaflet.domain.service.StructuredTemplateParser;

import java.util.Map;
import java.util.Optional;

final class UnsafeFragmentInsertionDetector {

    private final StructuredTemplateParser templateParser;

    UnsafeFragmentInsertionDetector() {
        this(new StructuredTemplateParser());
    }

    UnsafeFragmentInsertionDetector(StructuredTemplateParser templateParser) {
        this.templateParser = templateParser;
    }

    Optional<String> findUnsafeParameter(String templateSource, Map<String, Object> mergedParameters) {
        StructuredTemplateParser.ParsedTemplate parsedTemplate = templateParser
            .parseWithDiagnostics(templateSource)
            .parsedTemplate();
        if (parsedTemplate.elements().isEmpty()) {
            return Optional.empty();
        }

        return mergedParameters.entrySet().stream()
            .filter(entry -> isUnsafeCandidateValue(entry.getValue()))
            .map(Map.Entry::getKey)
            .filter(parameterName -> hasDynamicInsertionAttribute(parsedTemplate, parameterName))
            .findFirst();
    }

    private boolean isUnsafeCandidateValue(Object value) {
        if (!(value instanceof String stringValue)) {
            return false;
        }
        String trimmedValue = stringValue.trim();
        return !(trimmedValue.startsWith("~{") && trimmedValue.endsWith("}"));
    }

    private boolean hasDynamicInsertionAttribute(
        StructuredTemplateParser.ParsedTemplate parsedTemplate,
        String parameterName
    ) {
        return parsedTemplate.elements().stream()
            .flatMap(element -> element.attributes().stream())
            .filter(attribute -> attribute.hasValue())
            .filter(attribute -> FragmentReferenceAttributes.isInsertionAttribute(attribute.name()))
            .anyMatch(attribute -> isParameterExpression(attribute.value(), parameterName));
    }

    private boolean isParameterExpression(String attributeValue, String parameterName) {
        String trimmedValue = attributeValue.trim();
        if (!trimmedValue.startsWith("${") || !trimmedValue.endsWith("}")) {
            return false;
        }
        String expressionBody = trimmedValue.substring(2, trimmedValue.length() - 1).trim();
        return expressionBody.equals(parameterName);
    }
}
