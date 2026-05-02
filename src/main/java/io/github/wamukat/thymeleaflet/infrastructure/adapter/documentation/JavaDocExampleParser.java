package io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation;

import io.github.wamukat.thymeleaflet.domain.service.StructuredTemplateParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

final class JavaDocExampleParser {

    private static final Logger logger = LoggerFactory.getLogger(JavaDocExampleParser.class);
    private static final Set<String> EXAMPLE_REPLACE_ATTRIBUTES = Set.of("th:replace", "data-th-replace");

    private final StructuredTemplateParser templateParser;

    JavaDocExampleParser(StructuredTemplateParser templateParser) {
        this.templateParser = Objects.requireNonNull(templateParser, "templateParser cannot be null");
    }

    List<JavaDocAnalyzer.ExampleInfo> parse(String javadocContent) {
        List<JavaDocAnalyzer.ExampleInfo> examples = new ArrayList<>();

        for (String exampleMarkup : extractExampleMarkup(javadocContent)) {
            StructuredTemplateParser.ParsedTemplate parsedTemplate = parseExampleMarkup(exampleMarkup);
            for (StructuredTemplateParser.TemplateElement element : parsedTemplate.elements()) {
                for (StructuredTemplateParser.TemplateAttribute attribute : element.attributes()) {
                    String normalizedName = attribute.name().toLowerCase(java.util.Locale.ROOT);
                    if (!attribute.hasValue() || !EXAMPLE_REPLACE_ATTRIBUTES.contains(normalizedName)) {
                        continue;
                    }
                    parseExampleReference(attribute.value()).ifPresent(examples::add);
                }
            }
        }
        return examples;
    }

    private List<String> extractExampleMarkup(String javadocContent) {
        List<String> examples = new ArrayList<>();
        StringBuilder currentExample = new StringBuilder();
        boolean collectingExample = false;
        String[] lines = javadocContent.split("\n");
        for (String rawLine : lines) {
            String line = normalizeJavadocLine(rawLine);

            if (line.startsWith("@example")) {
                addExampleIfPresent(examples, currentExample);
                currentExample.setLength(0);
                collectingExample = true;
                String exampleBody = line.substring("@example".length()).trim();
                appendMarkupLine(currentExample, exampleBody);
                continue;
            }

            if (!collectingExample) {
                continue;
            }
            if (line.startsWith("@")) {
                addExampleIfPresent(examples, currentExample);
                currentExample.setLength(0);
                collectingExample = false;
                continue;
            }
            appendMarkupLine(currentExample, line);
        }
        addExampleIfPresent(examples, currentExample);
        return examples;
    }

    private String normalizeJavadocLine(String rawLine) {
        String line = rawLine.trim();
        if (line.startsWith("*")) {
            return line.substring(1).trim();
        }
        return line;
    }

    private void appendMarkupLine(StringBuilder exampleMarkup, String line) {
        if (line.isBlank()) {
            return;
        }
        if (exampleMarkup.isEmpty()) {
            int markupStart = line.indexOf('<');
            if (markupStart < 0) {
                return;
            }
            exampleMarkup.append(line.substring(markupStart));
            return;
        }
        exampleMarkup.append('\n').append(line);
    }

    private void addExampleIfPresent(List<String> examples, StringBuilder exampleMarkup) {
        if (!exampleMarkup.isEmpty()) {
            examples.add(exampleMarkup.toString());
        }
    }

    private StructuredTemplateParser.ParsedTemplate parseExampleMarkup(String exampleMarkup) {
        try {
            return templateParser.parse(exampleMarkup);
        } catch (IllegalArgumentException parseFailure) {
            logger.debug("Failed to parse @example markup: {}", exampleMarkup, parseFailure);
            return new StructuredTemplateParser.ParsedTemplate(List.of(), List.of(), List.of());
        }
    }

    private Optional<JavaDocAnalyzer.ExampleInfo> parseExampleReference(String rawReference) {
        String expression = rawReference.trim();
        if (expression.startsWith("~{") && expression.endsWith("}")) {
            expression = expression.substring(2, expression.length() - 1).trim();
        }
        int fragmentSeparator = expression.indexOf("::");
        if (fragmentSeparator <= 0) {
            return Optional.empty();
        }
        String templatePath = unquote(expression.substring(0, fragmentSeparator).trim());
        String fragmentExpression = expression.substring(fragmentSeparator + 2).trim();
        if (templatePath.isBlank() || fragmentExpression.isBlank()) {
            return Optional.empty();
        }
        String fragmentName = fragmentExpression;
        String argumentsStr = "";
        int openParen = fragmentExpression.indexOf('(');
        if (openParen >= 0) {
            int closeParen = fragmentExpression.lastIndexOf(')');
            if (closeParen < openParen) {
                return Optional.empty();
            }
            fragmentName = fragmentExpression.substring(0, openParen).trim();
            argumentsStr = fragmentExpression.substring(openParen + 1, closeParen).trim();
        }
        if (fragmentName.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(JavaDocAnalyzer.ExampleInfo.of(templatePath, fragmentName, parseArguments(argumentsStr)));
    }

    private String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2
            && ((trimmed.startsWith("'") && trimmed.endsWith("'"))
            || (trimmed.startsWith("\"") && trimmed.endsWith("\"")))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private List<String> parseArguments(String argumentsStr) {
        if (argumentsStr.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> arguments = new ArrayList<>();
        String[] parts = argumentsStr.split(",(?=(?:[^']*'[^']*')*[^']*$)");
        for (String part : parts) {
            arguments.add(part.trim());
        }

        return arguments;
    }
}
