package io.github.wamukat.thymeleaflet.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Tracks quotes and nested Thymeleaf expression syntax while scanning for top-level tokens.
 */
public final class TopLevelSyntaxScanner {

    public OptionalInt findFirst(String value, String token) {
        return findFirst(value, token, 0, value.length());
    }

    public OptionalInt findFirst(String value, String token, int startInclusive, int endExclusive) {
        Objects.requireNonNull(value, "value cannot be null");
        Objects.requireNonNull(token, "token cannot be null");
        if (token.isEmpty()) {
            return OptionalInt.empty();
        }
        if (startInclusive < 0 || endExclusive > value.length() || startInclusive > endExclusive) {
            throw new IllegalArgumentException("Invalid scan bounds");
        }

        ScanState state = new ScanState();
        for (int index = startInclusive; index < endExclusive; index++) {
            if (!state.isInsideNestedSyntax()
                && index + token.length() <= endExclusive
                && value.startsWith(token, index)) {
                return OptionalInt.of(index);
            }
            state.accept(value.charAt(index));
        }
        return OptionalInt.empty();
    }

    public SplitResult split(String value, char separator) {
        Objects.requireNonNull(value, "value cannot be null");
        List<String> segments = new ArrayList<>();
        ScanState state = new ScanState();
        int segmentStart = 0;

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (!state.isInsideNestedSyntax() && current == separator) {
                segments.add(value.substring(segmentStart, index));
                segmentStart = index + 1;
                continue;
            }
            state.accept(current);
        }
        segments.add(value.substring(segmentStart));
        return new SplitResult(segments, state.isBalanced());
    }

    public record SplitResult(List<String> segments, boolean isBalanced) {
        public SplitResult {
            segments = List.copyOf(segments);
        }
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
