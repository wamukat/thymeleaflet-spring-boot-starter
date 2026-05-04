package io.github.wamukat.thymeleaflet.domain.service;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class TemplateExpressionPathExtractor {

    private static final Set<String> RESERVED_ROOTS = Set.of(
        "true", "false", "null",
        "param", "session", "application", "request", "response",
        "and", "or", "not", "eq", "ne", "lt", "le", "gt", "ge",
        "instanceof", "matches", "div", "mod"
    );

    private TemplateExpressionPathExtractor() {
    }

    static List<List<String>> modelPaths(String expression, Set<String> excludedIdentifiers) {
        return modelPaths(expression, excludedIdentifiers, null);
    }

    static List<List<String>> modelPaths(
        String expression,
        Set<String> excludedIdentifiers,
        @Nullable List<List<String>> noArgMethodPaths
    ) {
        ExpressionPathParser parser = new ExpressionPathParser(
            ThymeleafExpressionTokenizer.tokenize(expression),
            excludedIdentifiers,
            noArgMethodPaths
        );
        return parser.parse();
    }

    static Optional<List<String>> directModelPath(String expression, Set<String> excludedIdentifiers) {
        List<ExpressionToken> tokens = ThymeleafExpressionTokenizer.tokenize(expression);
        if (tokens.isEmpty() || tokens.getFirst().type() != ExpressionTokenType.IDENTIFIER) {
            return Optional.empty();
        }
        String root = tokens.getFirst().text();
        if (RESERVED_ROOTS.contains(root) || excludedIdentifiers.contains(root) || "T".equals(root)) {
            return Optional.empty();
        }
        List<String> path = new ArrayList<>();
        path.add(root);
        int cursor = 1;
        while (cursor < tokens.size()) {
            ExpressionTokenType type = tokens.get(cursor).type();
            if (type == ExpressionTokenType.DOT || type == ExpressionTokenType.SAFE_DOT) {
                if (cursor + 1 >= tokens.size()
                    || tokens.get(cursor + 1).type() != ExpressionTokenType.IDENTIFIER
                    || cursor + 2 < tokens.size() && tokens.get(cursor + 2).type() == ExpressionTokenType.LEFT_PAREN) {
                    return Optional.empty();
                }
                path.add(tokens.get(cursor + 1).text());
                cursor += 2;
                continue;
            }
            if (type == ExpressionTokenType.LEFT_BRACKET) {
                Optional<BracketPathSegment> segment = directBracketSegment(tokens, cursor);
                if (segment.isEmpty()) {
                    return Optional.empty();
                }
                path.add(segment.orElseThrow().segment());
                cursor = segment.orElseThrow().nextIndex();
                continue;
            }
            return Optional.empty();
        }
        return Optional.of(path);
    }

    private static Optional<BracketPathSegment> directBracketSegment(
        List<ExpressionToken> tokens,
        int openBracketIndex
    ) {
        if (openBracketIndex + 2 >= tokens.size()
            || tokens.get(openBracketIndex).type() != ExpressionTokenType.LEFT_BRACKET
            || tokens.get(openBracketIndex + 2).type() != ExpressionTokenType.RIGHT_BRACKET) {
            return Optional.empty();
        }
        ExpressionToken key = tokens.get(openBracketIndex + 1);
        if (key.type() == ExpressionTokenType.STRING) {
            return Optional.of(new BracketPathSegment(key.text(), openBracketIndex + 3));
        }
        if (key.type() == ExpressionTokenType.NUMBER) {
            return Optional.of(new BracketPathSegment("[]", openBracketIndex + 3));
        }
        if (key.type() == ExpressionTokenType.IDENTIFIER && key.text().contains("-")) {
            return Optional.of(new BracketPathSegment(key.text(), openBracketIndex + 3));
        }
        return Optional.empty();
    }

    private enum ExpressionTokenType {
        IDENTIFIER,
        NUMBER,
        STRING,
        DOT,
        SAFE_DOT,
        LEFT_PAREN,
        RIGHT_PAREN,
        LEFT_BRACKET,
        RIGHT_BRACKET,
        HASH,
        AT,
        OTHER
    }

    private record ExpressionToken(ExpressionTokenType type, String text) {
    }

    private record BracketPathSegment(String segment, int nextIndex) {
    }

    private static final class ThymeleafExpressionTokenizer {

        private static List<ExpressionToken> tokenize(String expression) {
            List<ExpressionToken> tokens = new ArrayList<>();
            int index = 0;
            while (index < expression.length()) {
                char current = expression.charAt(index);
                if (Character.isWhitespace(current)) {
                    index++;
                    continue;
                }
                if (Character.isLetter(current) || current == '_') {
                    int start = index;
                    index++;
                    while (index < expression.length()) {
                        char part = expression.charAt(index);
                        if (!Character.isLetterOrDigit(part) && part != '_' && part != '-') {
                            break;
                        }
                        index++;
                    }
                    tokens.add(new ExpressionToken(ExpressionTokenType.IDENTIFIER, expression.substring(start, index)));
                    continue;
                }
                if (Character.isDigit(current)) {
                    int start = index;
                    index++;
                    while (index < expression.length() && Character.isDigit(expression.charAt(index))) {
                        index++;
                    }
                    tokens.add(new ExpressionToken(ExpressionTokenType.NUMBER, expression.substring(start, index)));
                    continue;
                }
                if (current == '\'' || current == '"') {
                    ParseStringResult stringResult = consumeString(expression, index);
                    tokens.add(new ExpressionToken(ExpressionTokenType.STRING, stringResult.content()));
                    index = stringResult.nextIndex();
                    continue;
                }
                if (current == '?' && index + 1 < expression.length() && expression.charAt(index + 1) == '.') {
                    tokens.add(new ExpressionToken(ExpressionTokenType.SAFE_DOT, "?."));
                    index += 2;
                    continue;
                }
                switch (current) {
                    case '.' -> tokens.add(new ExpressionToken(ExpressionTokenType.DOT, "."));
                    case '(' -> tokens.add(new ExpressionToken(ExpressionTokenType.LEFT_PAREN, "("));
                    case ')' -> tokens.add(new ExpressionToken(ExpressionTokenType.RIGHT_PAREN, ")"));
                    case '[' -> tokens.add(new ExpressionToken(ExpressionTokenType.LEFT_BRACKET, "["));
                    case ']' -> tokens.add(new ExpressionToken(ExpressionTokenType.RIGHT_BRACKET, "]"));
                    case '#' -> tokens.add(new ExpressionToken(ExpressionTokenType.HASH, "#"));
                    case '@' -> tokens.add(new ExpressionToken(ExpressionTokenType.AT, "@"));
                    default -> tokens.add(new ExpressionToken(ExpressionTokenType.OTHER, Character.toString(current)));
                }
                index++;
            }
            return tokens;
        }

        private static ParseStringResult consumeString(String expression, int start) {
            char quote = expression.charAt(start);
            StringBuilder content = new StringBuilder();
            int index = start + 1;
            boolean escaped = false;
            while (index < expression.length()) {
                char current = expression.charAt(index);
                if (escaped) {
                    content.append(current);
                    escaped = false;
                    index++;
                    continue;
                }
                if (current == '\\') {
                    escaped = true;
                    index++;
                    continue;
                }
                if (current == quote) {
                    return new ParseStringResult(content.toString(), index + 1);
                }
                content.append(current);
                index++;
            }
            return new ParseStringResult(content.toString(), expression.length());
        }

        private record ParseStringResult(String content, int nextIndex) {
        }
    }

    private static final class ExpressionPathParser {

        private final List<ExpressionToken> tokens;
        private final Set<String> excludedIdentifiers;
        private final @Nullable List<List<String>> noArgMethodPaths;
        private int index;

        private ExpressionPathParser(
            List<ExpressionToken> tokens,
            Set<String> excludedIdentifiers,
            @Nullable List<List<String>> noArgMethodPaths
        ) {
            this.tokens = tokens;
            this.excludedIdentifiers = excludedIdentifiers;
            this.noArgMethodPaths = noArgMethodPaths;
        }

        private List<List<String>> parse() {
            List<List<String>> paths = new ArrayList<>();
            while (index < tokens.size()) {
                if (!isAt(ExpressionTokenType.IDENTIFIER)) {
                    index++;
                    continue;
                }
                parseIdentifierPath().ifPresent(paths::add);
            }
            return paths;
        }

        private Optional<List<String>> parseIdentifierPath() {
            String root = tokens.get(index).text();
            if (isUtilityIdentifier(index) || isChainedIdentifier(index)) {
                index++;
                return Optional.empty();
            }
            if ("T".equals(root) && isAt(index + 1, ExpressionTokenType.LEFT_PAREN)) {
                skipStaticClassReference();
                return Optional.empty();
            }
            if (RESERVED_ROOTS.contains(root) || excludedIdentifiers.contains(root)) {
                index++;
                return Optional.empty();
            }
            if (isAt(index + 1, ExpressionTokenType.LEFT_PAREN)) {
                index++;
                return Optional.empty();
            }

            List<String> segments = new ArrayList<>();
            segments.add(root);
            index++;
            boolean endedWithNoArgMethodCall = false;

            while (index < tokens.size()) {
                if (isAt(ExpressionTokenType.LEFT_BRACKET)) {
                    Optional<String> bracketSegment = parseBracketSegment();
                    if (bracketSegment.isPresent()) {
                        segments.add(bracketSegment.orElseThrow());
                        continue;
                    }
                    skipUnsupportedBracket();
                    break;
                }
                if (!isAt(ExpressionTokenType.DOT) && !isAt(ExpressionTokenType.SAFE_DOT)) {
                    break;
                }
                if (!isAt(index + 1, ExpressionTokenType.IDENTIFIER)) {
                    break;
                }
                String propertyName = tokens.get(index + 1).text();
                int propertyIndex = index + 1;
                if (isAt(propertyIndex + 1, ExpressionTokenType.LEFT_PAREN)) {
                    int closeParen = findClosingParen(propertyIndex + 1);
                    if (closeParen > propertyIndex && isNoArgFunctionCall(propertyIndex + 1, closeParen)) {
                        List<String> methodPath = new ArrayList<>(segments);
                        methodPath.add(propertyName);
                        if (noArgMethodPaths != null) {
                            noArgMethodPaths.add(List.copyOf(methodPath));
                        }
                        endedWithNoArgMethodCall = true;
                        index = closeParen + 1;
                    } else {
                        index = propertyIndex + 1;
                    }
                    break;
                }
                segments.add(propertyName);
                index = propertyIndex + 1;
            }

            if (endedWithNoArgMethodCall) {
                return Optional.empty();
            }
            return Optional.of(segments);
        }

        private Optional<String> parseBracketSegment() {
            if (isAt(index, ExpressionTokenType.LEFT_BRACKET)
                && isAt(index + 1, ExpressionTokenType.STRING)
                && isAt(index + 2, ExpressionTokenType.RIGHT_BRACKET)) {
                String key = tokens.get(index + 1).text();
                index += 3;
                return Optional.of(key);
            }
            if (isAt(index, ExpressionTokenType.LEFT_BRACKET)
                && isAt(index + 1, ExpressionTokenType.NUMBER)
                && isAt(index + 2, ExpressionTokenType.RIGHT_BRACKET)) {
                index += 3;
                return Optional.of("[]");
            }
            if (isAt(index, ExpressionTokenType.LEFT_BRACKET)
                && isAt(index + 1, ExpressionTokenType.IDENTIFIER)
                && isAt(index + 2, ExpressionTokenType.RIGHT_BRACKET)) {
                String key = tokens.get(index + 1).text();
                if (key.contains("-")) {
                    index += 3;
                    return Optional.of(key);
                }
            }
            return Optional.empty();
        }

        private void skipUnsupportedBracket() {
            if (!isAt(ExpressionTokenType.LEFT_BRACKET)) {
                return;
            }
            int depth = 0;
            while (index < tokens.size()) {
                if (isAt(ExpressionTokenType.LEFT_BRACKET)) {
                    depth++;
                } else if (isAt(ExpressionTokenType.RIGHT_BRACKET)) {
                    depth--;
                    if (depth == 0) {
                        index++;
                        return;
                    }
                }
                index++;
            }
        }

        private boolean isUtilityIdentifier(int tokenIndex) {
            return isAt(tokenIndex - 1, ExpressionTokenType.HASH) || isAt(tokenIndex - 1, ExpressionTokenType.AT);
        }

        private boolean isChainedIdentifier(int tokenIndex) {
            return isAt(tokenIndex - 1, ExpressionTokenType.DOT) || isAt(tokenIndex - 1, ExpressionTokenType.SAFE_DOT);
        }

        private void skipStaticClassReference() {
            int closeParen = findClosingParen(index + 1);
            if (closeParen < 0) {
                index++;
                return;
            }
            index = closeParen + 1;
            while (index + 3 < tokens.size()
                && (isAt(index, ExpressionTokenType.DOT) || isAt(index, ExpressionTokenType.SAFE_DOT))
                && isAt(index + 1, ExpressionTokenType.IDENTIFIER)
                && isAt(index + 2, ExpressionTokenType.LEFT_PAREN)) {
                int methodCloseParen = findClosingParen(index + 2);
                if (methodCloseParen < 0 || !isNoArgFunctionCall(index + 2, methodCloseParen)) {
                    index++;
                    return;
                }
                index = methodCloseParen + 1;
            }
        }

        private int findClosingParen(int openParenIndex) {
            int depth = 0;
            for (int cursor = openParenIndex; cursor < tokens.size(); cursor++) {
                ExpressionTokenType type = tokens.get(cursor).type();
                if (type == ExpressionTokenType.LEFT_PAREN) {
                    depth++;
                } else if (type == ExpressionTokenType.RIGHT_PAREN) {
                    depth--;
                    if (depth == 0) {
                        return cursor;
                    }
                }
            }
            return -1;
        }

        private boolean isNoArgFunctionCall(int openParenIndex, int closeParenIndex) {
            return closeParenIndex == openParenIndex + 1;
        }

        private boolean isAt(ExpressionTokenType type) {
            return isAt(index, type);
        }

        private boolean isAt(int tokenIndex, ExpressionTokenType type) {
            return tokenIndex >= 0 && tokenIndex < tokens.size() && tokens.get(tokenIndex).type() == type;
        }
    }
}
