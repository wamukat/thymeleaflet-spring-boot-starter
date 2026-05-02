package io.github.wamukat.thymeleaflet.domain.service;

import io.github.wamukat.thymeleaflet.domain.model.FragmentExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FragmentExpressionParser {

    public Optional<FragmentExpression> parse(String rawExpression) {
        if (rawExpression == null || rawExpression.isBlank()) {
            return Optional.empty();
        }
        String expression = unwrapFragmentExpression(rawExpression.trim());
        if (expression.isBlank()
            || expression.startsWith("${")
            || expression.startsWith("*{")
            || expression.startsWith("#{")) {
            return Optional.empty();
        }

        int separatorIndex = findTopLevelFragmentSeparator(expression);
        if (separatorIndex <= 0 || separatorIndex >= expression.length() - 2) {
            return Optional.empty();
        }

        String templatePath = normalizeTemplatePath(expression.substring(0, separatorIndex));
        Optional<FragmentSelector> selector = parseFragmentSelector(expression.substring(separatorIndex + 2));
        if (templatePath.isBlank() || selector.isEmpty()) {
            return Optional.empty();
        }
        FragmentSelector resolvedSelector = selector.orElseThrow();
        return Optional.of(FragmentExpression.of(
            templatePath,
            resolvedSelector.name(),
            resolvedSelector.arguments(),
            resolvedSelector.hasArgumentList()
        ));
    }

    private String unwrapFragmentExpression(String rawExpression) {
        if (rawExpression.startsWith("~{") && rawExpression.endsWith("}")) {
            return rawExpression.substring(2, rawExpression.length() - 1).trim();
        }
        return rawExpression;
    }

    private int findTopLevelFragmentSeparator(String expression) {
        ScanState state = new ScanState();
        for (int index = 0; index < expression.length() - 1; index++) {
            state.accept(expression.charAt(index));
            if (!state.isInsideNestedSyntax()
                && expression.charAt(index) == ':'
                && expression.charAt(index + 1) == ':') {
                return index;
            }
        }
        return -1;
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
        ScanState state = new ScanState();
        int segmentStart = 0;

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            state.accept(current);
            if (!state.isInsideNestedSyntax() && current == separator) {
                addSegment(segments, value.substring(segmentStart, index));
                segmentStart = index + 1;
            }
        }
        if (!state.isBalanced()) {
            return Optional.empty();
        }
        addSegment(segments, value.substring(segmentStart));
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

    private static final class ScanState {
        private int depthParen;
        private int depthBracket;
        private int depthBrace;
        private boolean inSingleQuote;
        private boolean inDoubleQuote;
        private char previous;

        private void accept(char current) {
            if (current == '\'' && !inDoubleQuote && previous != '\\') {
                inSingleQuote = !inSingleQuote;
                previous = current;
                return;
            }
            if (current == '"' && !inSingleQuote && previous != '\\') {
                inDoubleQuote = !inDoubleQuote;
                previous = current;
                return;
            }
            if (!inSingleQuote && !inDoubleQuote) {
                if (current == '(') {
                    depthParen++;
                } else if (current == ')' && depthParen > 0) {
                    depthParen--;
                } else if (current == '[') {
                    depthBracket++;
                } else if (current == ']' && depthBracket > 0) {
                    depthBracket--;
                } else if (current == '{') {
                    depthBrace++;
                } else if (current == '}' && depthBrace > 0) {
                    depthBrace--;
                }
            }
            previous = current;
        }

        private boolean isInsideNestedSyntax() {
            return inSingleQuote || inDoubleQuote || depthParen > 0 || depthBracket > 0 || depthBrace > 0;
        }

        private boolean isBalanced() {
            return !inSingleQuote && !inDoubleQuote && depthParen == 0 && depthBracket == 0 && depthBrace == 0;
        }
    }
}
