package io.github.wamukat.thymeleaflet.domain.service;

import io.github.wamukat.thymeleaflet.domain.model.ModelPath;
import io.github.wamukat.thymeleaflet.domain.model.FragmentExpression;
import io.github.wamukat.thymeleaflet.domain.model.TemplateInference;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * テンプレート式を解析し、モデル推論に必要な情報を抽出する。
 */
public class TemplateModelExpressionAnalyzer {

    private static final Set<String> RESERVED_ROOTS = Set.of(
        "true", "false", "null",
        "param", "session", "application", "request", "response",
        "and", "or", "not", "eq", "ne", "lt", "le", "gt", "ge",
        "instanceof", "matches", "div", "mod"
    );
    private final StructuredTemplateParser templateParser;
    private final FragmentExpressionParser fragmentExpressionParser;

    public TemplateModelExpressionAnalyzer() {
        this(new StructuredTemplateParser(), new FragmentExpressionParser());
    }

    TemplateModelExpressionAnalyzer(StructuredTemplateParser templateParser) {
        this(templateParser, new FragmentExpressionParser());
    }

    TemplateModelExpressionAnalyzer(
        StructuredTemplateParser templateParser,
        FragmentExpressionParser fragmentExpressionParser
    ) {
        this.templateParser = templateParser;
        this.fragmentExpressionParser = fragmentExpressionParser;
    }

    public TemplateInference analyze(String html, Set<String> parameterNames) {
        StructuredTemplateParser.ParsedTemplate template = templateParser.parse(html);
        Set<String> excludedIdentifiers = new HashSet<>(parameterNames);
        excludedIdentifiers.addAll(extractLocalVariablesFromThWith(template));
        Map<String, ModelPath> loopVariablePaths = extractLoopVariablePaths(template);
        List<String> expressionSources = expressionSources(template);
        List<ModelPath> modelPaths = extractModelPathsFromSources(expressionSources, excludedIdentifiers);
        List<ModelPath> noArgMethodPaths = extractNoArgMethodPathsFromSources(expressionSources, excludedIdentifiers);
        Map<String, Boolean> referencedTemplatePaths = extractReferencedTemplatePaths(template);
        return new TemplateInference(modelPaths, loopVariablePaths, referencedTemplatePaths, noArgMethodPaths);
    }

    private List<ModelPath> extractModelPathsFromSources(List<String> sources, Set<String> excludedIdentifiers) {
        List<ModelPath> paths = new ArrayList<>();
        for (String source : sources) {
            for (String expression : extractExpressionBodies(source)) {
                for (List<String> path : extractModelPaths(expression, excludedIdentifiers)) {
                    paths.add(ModelPath.of(path));
                }
            }
        }
        return paths;
    }

    private List<ModelPath> extractNoArgMethodPathsFromSources(List<String> sources, Set<String> excludedIdentifiers) {
        LinkedHashSet<ModelPath> methodPaths = new LinkedHashSet<>();
        for (String source : sources) {
            for (String expression : extractExpressionBodies(source)) {
                List<List<String>> extracted = new ArrayList<>();
                extractModelPaths(expression, excludedIdentifiers, extracted);
                for (List<String> path : extracted) {
                    methodPaths.add(ModelPath.of(path));
                }
            }
        }
        return new ArrayList<>(methodPaths);
    }

    private List<String> extractExpressionBodies(String source) {
        List<String> expressions = new ArrayList<>();
        int index = 0;
        while (index < source.length() - 1) {
            char current = source.charAt(index);
            if ((current != '$' && current != '*') || source.charAt(index + 1) != '{') {
                index++;
                continue;
            }
            Optional<ExpressionBody> expressionBody = readExpressionBody(source, index + 2);
            if (expressionBody.isEmpty()) {
                index += 2;
                continue;
            }
            ExpressionBody resolvedBody = expressionBody.orElseThrow();
            expressions.add(resolvedBody.content());
            index = resolvedBody.nextIndex();
        }
        return expressions;
    }

    private Optional<ExpressionBody> readExpressionBody(String source, int bodyStart) {
        int depthBrace = 1;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escaped = false;
        for (int index = bodyStart; index < source.length(); index++) {
            char current = source.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (current == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }
            if (current == '{') {
                depthBrace++;
            } else if (current == '}') {
                depthBrace--;
                if (depthBrace == 0) {
                    return Optional.of(new ExpressionBody(source.substring(bodyStart, index), index + 1));
                }
            }
        }
        return Optional.empty();
    }

    private List<List<String>> extractModelPaths(String expression, Set<String> excludedIdentifiers) {
        return extractModelPaths(expression, excludedIdentifiers, null);
    }

    private List<List<String>> extractModelPaths(
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

    private Map<String, ModelPath> extractLoopVariablePaths(StructuredTemplateParser.ParsedTemplate template) {
        Map<String, ModelPath> loopVariables = new LinkedHashMap<>();
        for (String raw : thymeleafAttributeValues(template, Set.of("th:each", "data-th-each"))) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            int separator = raw.indexOf(':');
            if (separator <= 0 || separator >= raw.length() - 1) {
                continue;
            }
            String variablePart = raw.substring(0, separator).trim();
            String iterablePart = raw.substring(separator + 1).trim();
            List<String> aliases = extractLoopAliases(variablePart);
            if (aliases.isEmpty()) {
                continue;
            }
            String iterableExpression = iterablePart;
            if (iterableExpression.startsWith("${") && iterableExpression.endsWith("}")) {
                iterableExpression = iterableExpression.substring(2, iterableExpression.length() - 1);
            }
            List<List<String>> iterablePaths = extractModelPaths(iterableExpression, Set.of());
            if (iterablePaths.isEmpty()) {
                continue;
            }
            List<String> iterablePath = iterablePaths.getFirst();
            if (iterablePath.isEmpty()) {
                continue;
            }
            for (String alias : aliases) {
                loopVariables.putIfAbsent(alias, ModelPath.of(iterablePath));
            }
        }
        return loopVariables;
    }

    private Map<String, Boolean> extractReferencedTemplatePaths(StructuredTemplateParser.ParsedTemplate template) {
        Map<String, Boolean> referencedTemplatePaths = new LinkedHashMap<>();
        for (String raw : thymeleafAttributeValues(template, Set.of(
            "th:replace", "th:insert", "data-th-replace", "data-th-insert"
        ))) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            fragmentExpressionParser.parse(raw)
                .ifPresent(expression -> referencedTemplatePaths.merge(
                    expression.templatePath(),
                    requiresChildModelRecursion(expression),
                    (left, right) -> left || right
                ));
        }
        return referencedTemplatePaths;
    }

    private boolean requiresChildModelRecursion(FragmentExpression expression) {
        if (expression.arguments().isEmpty()) {
            return !expression.hasArgumentList();
        }
        for (String argument : expression.arguments()) {
            if (argument.isBlank()) {
                continue;
            }
            String value = argument;
            int assignIndex = argument.indexOf('=');
            if (assignIndex >= 0 && assignIndex < argument.length() - 1) {
                value = argument.substring(assignIndex + 1).trim();
            }
            if (!isLiteralExpression(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLiteralExpression(String value) {
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return true;
        }
        if (normalized.startsWith("'") && normalized.endsWith("'") && normalized.length() >= 2) {
            return true;
        }
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() >= 2) {
            return true;
        }
        if (normalized.equals("true") || normalized.equals("false") || normalized.equals("null")) {
            return true;
        }
        if (normalized.matches("[-+]?\\d+(\\.\\d+)?")) {
            return true;
        }
        return false;
    }

    private Set<String> extractLocalVariablesFromThWith(StructuredTemplateParser.ParsedTemplate template) {
        Set<String> localVariables = new HashSet<>();
        for (String raw : thymeleafAttributeValues(template, Set.of("th:with", "data-th-with"))) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            for (String assignment : splitTopLevel(raw, ',')) {
                int equalIndex = assignment.indexOf('=');
                if (equalIndex <= 0) {
                    continue;
                }
                String candidate = assignment.substring(0, equalIndex).trim();
                if (isValidIdentifier(candidate)) {
                    localVariables.add(candidate);
                }
            }
        }
        return localVariables;
    }

    private List<String> expressionSources(StructuredTemplateParser.ParsedTemplate template) {
        List<String> sources = new ArrayList<>();
        for (StructuredTemplateParser.TemplateElement element : template.elements()) {
            for (StructuredTemplateParser.TemplateAttribute attribute : element.attributes()) {
                if (attribute.hasValue()) {
                    sources.add(attribute.value());
                }
            }
        }
        for (StructuredTemplateParser.TemplateText text : template.textNodes()) {
            if (!text.content().isBlank()) {
                sources.add(text.content());
            }
        }
        return sources;
    }

    private List<String> thymeleafAttributeValues(
        StructuredTemplateParser.ParsedTemplate template,
        Set<String> attributeNames
    ) {
        List<String> values = new ArrayList<>();
        for (StructuredTemplateParser.TemplateElement element : template.elements()) {
            for (StructuredTemplateParser.TemplateAttribute attribute : element.attributes()) {
                if (!attribute.hasValue()) {
                    continue;
                }
                String normalizedName = attribute.name().toLowerCase(java.util.Locale.ROOT);
                if (attributeNames.contains(normalizedName)) {
                    values.add(attribute.value());
                }
            }
        }
        return values;
    }

    private List<String> extractLoopAliases(String variablePart) {
        String normalized = variablePart.trim();
        if (normalized.isEmpty()) {
            return List.of();
        }
        if (normalized.startsWith("(") && normalized.endsWith(")") && normalized.length() > 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        List<String> aliases = new ArrayList<>();
        for (String token : splitTopLevel(normalized, ',')) {
            String alias = token.trim();
            if (alias.isEmpty()) {
                continue;
            }
            int eqIndex = alias.indexOf('=');
            if (eqIndex > 0) {
                alias = alias.substring(0, eqIndex).trim();
            }
            if (isValidIdentifier(alias)) {
                aliases.add(alias);
            }
        }
        return aliases;
    }

    private boolean isValidIdentifier(String candidate) {
        if (candidate.isEmpty() || !isIdentifierStart(candidate.charAt(0))) {
            return false;
        }
        for (int i = 1; i < candidate.length(); i++) {
            if (!isIdentifierPart(candidate.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private List<String> splitTopLevel(String value, char separator) {
        List<String> segments = new ArrayList<>();
        int depthParen = 0;
        int depthBracket = 0;
        int depthBrace = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int segmentStart = 0;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            char previous = i > 0 ? value.charAt(i - 1) : '\0';
            if (c == '\'' && !inDoubleQuote && previous != '\\') {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (c == '"' && !inSingleQuote && previous != '\\') {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }
            if (c == '(') {
                depthParen++;
            } else if (c == ')' && depthParen > 0) {
                depthParen--;
            } else if (c == '[') {
                depthBracket++;
            } else if (c == ']' && depthBracket > 0) {
                depthBracket--;
            } else if (c == '{') {
                depthBrace++;
            } else if (c == '}' && depthBrace > 0) {
                depthBrace--;
            } else if (c == separator && depthParen == 0 && depthBracket == 0 && depthBrace == 0) {
                segments.add(value.substring(segmentStart, i).trim());
                segmentStart = i + 1;
            }
        }
        if (segmentStart <= value.length()) {
            segments.add(value.substring(segmentStart).trim());
        }
        return segments;
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }

    private enum ExpressionTokenType {
        IDENTIFIER,
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

    private record ExpressionBody(String content, int nextIndex) {
    }

    private record ExpressionToken(ExpressionTokenType type, String text) {
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

    private final class ExpressionPathParser {

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
                    Optional<String> bracketKey = parseBracketKey();
                    if (bracketKey.isPresent()) {
                        segments.add(bracketKey.orElseThrow());
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

        private Optional<String> parseBracketKey() {
            if (isAt(index, ExpressionTokenType.LEFT_BRACKET)
                && isAt(index + 1, ExpressionTokenType.STRING)
                && isAt(index + 2, ExpressionTokenType.RIGHT_BRACKET)) {
                String key = tokens.get(index + 1).text();
                index += 3;
                return Optional.of(key);
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
