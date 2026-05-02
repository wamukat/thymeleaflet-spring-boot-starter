package io.github.wamukat.thymeleaflet.domain.service;

import io.github.wamukat.thymeleaflet.domain.model.FragmentExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class FragmentExpressionParser {

    private final TopLevelSyntaxScanner topLevelSyntaxScanner = new TopLevelSyntaxScanner();

    public Optional<FragmentExpression> parse(String rawExpression) {
        return parseWithDiagnostics(rawExpression).expression();
    }

    public FragmentExpressionParseResult parseWithDiagnostics(String rawExpression) {
        if (rawExpression == null || rawExpression.isBlank()) {
            return FragmentExpressionParseResult.empty(
                ParserDiagnostic.warning("FRAGMENT_EXPRESSION_EMPTY", "Fragment expression is empty")
            );
        }
        String expression = unwrapFragmentExpression(rawExpression.trim());
        if (expression.isBlank()) {
            return FragmentExpressionParseResult.empty(
                ParserDiagnostic.warning("FRAGMENT_EXPRESSION_EMPTY", "Fragment expression is empty")
            );
        }
        if (expression.startsWith("${") || expression.startsWith("*{") || expression.startsWith("#{")) {
            return FragmentExpressionParseResult.empty(
                ParserDiagnostic.warning(
                    "FRAGMENT_EXPRESSION_DYNAMIC",
                    "Dynamic fragment expression was skipped: " + rawExpression.trim()
                )
            );
        }

        int separatorIndex = findTopLevelFragmentSeparator(expression);
        if (separatorIndex <= 0 || separatorIndex >= expression.length() - 2) {
            return FragmentExpressionParseResult.empty(
                ParserDiagnostic.warning(
                    "FRAGMENT_EXPRESSION_MALFORMED",
                    "Fragment expression is missing a valid template/fragment separator: " + rawExpression.trim()
                )
            );
        }

        String templatePath = normalizeTemplatePath(expression.substring(0, separatorIndex));
        Optional<FragmentSelector> selector = parseFragmentSelector(expression.substring(separatorIndex + 2));
        if (templatePath.isBlank() || selector.isEmpty()) {
            return FragmentExpressionParseResult.empty(
                ParserDiagnostic.warning(
                    "FRAGMENT_EXPRESSION_MALFORMED",
                    "Fragment expression could not be parsed: " + rawExpression.trim()
                )
            );
        }
        FragmentSelector resolvedSelector = selector.orElseThrow();
        return FragmentExpressionParseResult.success(
            FragmentExpression.of(
                templatePath,
                resolvedSelector.name(),
                resolvedSelector.arguments(),
                resolvedSelector.hasArgumentList()
            )
        );
    }

    private String unwrapFragmentExpression(String rawExpression) {
        if (rawExpression.startsWith("~{") && rawExpression.endsWith("}")) {
            return rawExpression.substring(2, rawExpression.length() - 1).trim();
        }
        return rawExpression;
    }

    private int findTopLevelFragmentSeparator(String expression) {
        return topLevelSyntaxScanner.findFirst(expression, "::").orElse(-1);
    }

    private String normalizeTemplatePath(String rawTemplatePath) {
        String templatePath = unquote(rawTemplatePath.trim());
        if (templatePath.startsWith("/") && templatePath.length() > 1) {
            templatePath = templatePath.substring(1);
        }
        if (templatePath.startsWith("#") || templatePath.startsWith("this") || templatePath.contains("${")) {
            return "";
        }
        return templatePath;
    }

    private Optional<FragmentSelector> parseFragmentSelector(String rawSelector) {
        String selector = unquote(rawSelector.trim());
        if (selector.isBlank()) {
            return Optional.empty();
        }
        int openParen = selector.indexOf('(');
        if (openParen < 0) {
            return Optional.of(new FragmentSelector(unquote(selector), List.of(), false));
        }
        int closeParen = selector.lastIndexOf(')');
        if (closeParen < openParen || closeParen != selector.length() - 1) {
            return Optional.empty();
        }
        String fragmentName = unquote(selector.substring(0, openParen).trim());
        if (fragmentName.isBlank()) {
            return Optional.empty();
        }
        Optional<List<String>> arguments = splitTopLevel(selector.substring(openParen + 1, closeParen), ',');
        return arguments.map(values -> new FragmentSelector(fragmentName, values, true));
    }

    private Optional<List<String>> splitTopLevel(String value, char separator) {
        List<String> segments = new ArrayList<>();
        TopLevelSyntaxScanner.SplitResult splitResult = topLevelSyntaxScanner.split(value, separator);
        if (!splitResult.isBalanced()) {
            return Optional.empty();
        }
        splitResult.segments().forEach(segment -> addSegment(segments, segment));
        return Optional.of(segments);
    }

    private void addSegment(List<String> segments, String segment) {
        String normalized = segment.trim();
        if (!normalized.isEmpty()) {
            segments.add(normalized);
        }
    }

    private String unquote(String value) {
        if (value.length() >= 2
            && ((value.startsWith("'") && value.endsWith("'"))
            || (value.startsWith("\"") && value.endsWith("\"")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private record FragmentSelector(String name, List<String> arguments, boolean hasArgumentList) {
    }

    public record FragmentExpressionParseResult(
        Optional<FragmentExpression> expression,
        List<ParserDiagnostic> diagnostics
    ) {
        public FragmentExpressionParseResult {
            expression = Objects.requireNonNull(expression, "expression cannot be null");
            diagnostics = List.copyOf(diagnostics);
        }

        private static FragmentExpressionParseResult success(FragmentExpression expression) {
            return new FragmentExpressionParseResult(Optional.of(expression), List.of());
        }

        private static FragmentExpressionParseResult empty(ParserDiagnostic diagnostic) {
            return new FragmentExpressionParseResult(Optional.empty(), List.of(diagnostic));
        }
    }

}
