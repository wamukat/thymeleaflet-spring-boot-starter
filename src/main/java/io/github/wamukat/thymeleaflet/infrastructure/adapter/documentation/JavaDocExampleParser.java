package io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation;

import io.github.wamukat.thymeleaflet.domain.model.FragmentExpression;
import io.github.wamukat.thymeleaflet.domain.service.FragmentExpressionParser;
import io.github.wamukat.thymeleaflet.domain.service.ParserDiagnostic;
import io.github.wamukat.thymeleaflet.domain.service.StructuredTemplateParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class JavaDocExampleParser {

    private static final Logger logger = LoggerFactory.getLogger(JavaDocExampleParser.class);
    private static final Set<String> EXAMPLE_REPLACE_ATTRIBUTES = Set.of("th:replace", "data-th-replace");

    private final StructuredTemplateParser templateParser;
    private final FragmentExpressionParser fragmentExpressionParser;

    JavaDocExampleParser(StructuredTemplateParser templateParser) {
        this.templateParser = Objects.requireNonNull(templateParser, "templateParser cannot be null");
        this.fragmentExpressionParser = new FragmentExpressionParser();
    }

    List<JavaDocAnalyzer.ExampleInfo> parse(String javadocContent) {
        return parseWithDiagnostics(javadocContent).examples();
    }

    ExampleParseResult parseWithDiagnostics(String javadocContent) {
        List<JavaDocAnalyzer.ExampleInfo> examples = new ArrayList<>();
        List<ParserDiagnostic> diagnostics = new ArrayList<>();

        for (String exampleMarkup : extractExampleMarkup(javadocContent)) {
            StructuredTemplateParser.TemplateParseResult parseResult = parseExampleMarkup(exampleMarkup);
            diagnostics.addAll(parseResult.diagnostics());
            StructuredTemplateParser.ParsedTemplate parsedTemplate = parseResult.parsedTemplate();
            for (StructuredTemplateParser.TemplateElement element : parsedTemplate.elements()) {
                for (StructuredTemplateParser.TemplateAttribute attribute : element.attributes()) {
                    String normalizedName = attribute.name().toLowerCase(java.util.Locale.ROOT);
                    if (!attribute.hasValue() || !EXAMPLE_REPLACE_ATTRIBUTES.contains(normalizedName)) {
                        continue;
                    }
                    FragmentExpressionParser.FragmentExpressionParseResult referenceResult =
                        fragmentExpressionParser.parseWithDiagnostics(attribute.value());
                    diagnostics.addAll(referenceResult.diagnostics());
                    referenceResult.expression()
                        .map(this::toExampleInfo)
                        .ifPresent(examples::add);
                }
            }
        }
        return new ExampleParseResult(examples, diagnostics);
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

    private StructuredTemplateParser.TemplateParseResult parseExampleMarkup(String exampleMarkup) {
        try {
            return templateParser.parseWithDiagnostics(exampleMarkup);
        } catch (IllegalArgumentException parseFailure) {
            logger.debug("Failed to parse @example markup: {}", exampleMarkup, parseFailure);
            return new StructuredTemplateParser.TemplateParseResult(
                new StructuredTemplateParser.ParsedTemplate(List.of(), List.of(), List.of()),
                List.of(ParserDiagnostic.warning("JAVADOC_EXAMPLE_MARKUP_MALFORMED", diagnosticMessage(parseFailure)))
            );
        }
    }

    private String diagnosticMessage(Exception failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure.getClass().getSimpleName();
        }
        return message;
    }

    private JavaDocAnalyzer.ExampleInfo toExampleInfo(FragmentExpression expression) {
        return JavaDocAnalyzer.ExampleInfo.of(
            expression.templatePath(),
            expression.fragmentName(),
            expression.arguments()
        );
    }

    record ExampleParseResult(List<JavaDocAnalyzer.ExampleInfo> examples, List<ParserDiagnostic> diagnostics) {
        ExampleParseResult {
            examples = List.copyOf(examples);
            diagnostics = List.copyOf(diagnostics);
        }
    }
}
