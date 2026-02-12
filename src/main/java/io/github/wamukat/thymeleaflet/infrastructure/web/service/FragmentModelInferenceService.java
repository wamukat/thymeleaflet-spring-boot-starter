package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.domain.model.InferredModel;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * テンプレート内の式からモデル値を推定するサービス。
 *
 * 推定値は Custom story の初期モデル候補として利用する。
 */
@Component
public class FragmentModelInferenceService {

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^}]*)}");
    private static final Pattern TH_WITH_PATTERN = Pattern.compile(
        "th:with\\s*=\\s*\"([^\"]*)\"|th:with\\s*=\\s*'([^']*)'"
    );
    private static final Pattern TH_EACH_PATTERN = Pattern.compile(
        "th:each\\s*=\\s*\"([^\"]*)\"|th:each\\s*=\\s*'([^']*)'"
    );
    private static final Pattern TH_REPLACE_OR_INSERT_PATTERN = Pattern.compile(
        "th:(?:replace|insert)\\s*=\\s*\"([^\"]*)\"|th:(?:replace|insert)\\s*=\\s*'([^']*)'"
    );
    private static final Set<String> RESERVED_ROOTS = Set.of(
        "true", "false", "null",
        "param", "session", "application", "request", "response"
    );

    private final ResourceLoader resourceLoader;

    public FragmentModelInferenceService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public Map<String, Object> inferModel(String templatePath, String fragmentName, List<String> parameterNames) {
        InferredModel inferred = inferModelRecursive(
            templatePath,
            parameterNames == null ? List.of() : parameterNames,
            new HashSet<>()
        );
        return inferred.toMap();
    }

    private List<List<String>> extractModelPaths(String expression, Set<String> excludedIdentifiers) {
        List<List<String>> paths = new ArrayList<>();
        int length = expression.length();
        int i = 0;
        while (i < length) {
            char current = expression.charAt(i);
            if (!isIdentifierStart(current)) {
                i++;
                continue;
            }
            if (isInvalidRootStartContext(expression, i)) {
                i++;
                continue;
            }

            int start = i;
            i++;
            while (i < length && isIdentifierPart(expression.charAt(i))) {
                i++;
            }

            String root = expression.substring(start, i);
            if (RESERVED_ROOTS.contains(root) || excludedIdentifiers.contains(root)) {
                continue;
            }
            if (isFunctionCall(expression, i)) {
                continue;
            }

            List<String> segments = new ArrayList<>();
            segments.add(root);

            while (i < length) {
                if (expression.startsWith("?.", i)) {
                    i += 2;
                } else if (expression.charAt(i) == '.') {
                    i++;
                } else if (expression.charAt(i) == '[') {
                    int end = consumeBracketAccessor(expression, i);
                    if (end <= i) {
                        break;
                    }
                    Optional<String> keyOptional = extractBracketKey(expression.substring(i, end + 1));
                    i = end + 1;
                    if (keyOptional.isEmpty()) {
                        continue;
                    }
                    segments.add(keyOptional.orElseThrow());
                    continue;
                } else {
                    break;
                }

                if (i >= length || !isIdentifierStart(expression.charAt(i))) {
                    break;
                }
                int propStart = i;
                i++;
                while (i < length && isIdentifierPart(expression.charAt(i))) {
                    i++;
                }
                if (isFunctionCall(expression, i)) {
                    break;
                }
                segments.add(expression.substring(propStart, i));
            }

            if (!segments.isEmpty()) {
                paths.add(segments);
            }
        }
        return paths;
    }

    private int consumeBracketAccessor(String expression, int start) {
        int i = start;
        int depth = 0;
        while (i < expression.length()) {
            char c = expression.charAt(i);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
            i++;
        }
        return -1;
    }

    private Optional<String> extractBracketKey(String accessor) {
        String trimmed = accessor.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return Optional.empty();
        }
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        if (inner.startsWith("'") && inner.endsWith("'") && inner.length() >= 2) {
            return Optional.of(inner.substring(1, inner.length() - 1));
        }
        if (inner.startsWith("\"") && inner.endsWith("\"") && inner.length() >= 2) {
            return Optional.of(inner.substring(1, inner.length() - 1));
        }
        return Optional.empty();
    }

    private InferredModel inferModelRecursive(
        String templatePath,
        List<String> parameterNames,
        Set<String> visitedTemplatePaths
    ) {
        if (!visitedTemplatePaths.add(templatePath)) {
            return new InferredModel();
        }
        String html = readTemplateSource(templatePath);
        if (html.isEmpty()) {
            return new InferredModel();
        }

        InferredModel inferred = inferCurrentTemplateModel(html, parameterNames);
        for (String referencedTemplatePath : extractReferencedTemplatePaths(html)) {
            if (referencedTemplatePath.equals(templatePath)) {
                continue;
            }
            InferredModel child = inferModelRecursive(referencedTemplatePath, List.of(), visitedTemplatePaths);
            inferred.merge(child);
        }
        return inferred;
    }

    private InferredModel inferCurrentTemplateModel(String html, List<String> parameterNames) {
        Set<String> excludedIdentifiers = new HashSet<>(parameterNames);
        excludedIdentifiers.addAll(extractLocalVariablesFromThWith(html));
        Map<String, List<String>> loopVariablePaths = extractLoopVariablePaths(html);
        InferredModel inferred = new InferredModel();

        Matcher matcher = EXPRESSION_PATTERN.matcher(html);
        while (matcher.find()) {
            String expression = stripStringLiterals(matcher.group(1));
            for (List<String> path : extractModelPaths(expression, excludedIdentifiers)) {
                if (path.isEmpty()) {
                    continue;
                }
                String root = path.getFirst();
                Object leafValue = inferLeafValue(path.get(path.size() - 1));
                if (loopVariablePaths.containsKey(root)) {
                    inferred.putLoopPath(loopVariablePaths.get(root), path.subList(1, path.size()), leafValue);
                    continue;
                }
                inferred.putPath(path, leafValue);
            }
        }
        return inferred;
    }

    private Set<String> extractReferencedTemplatePaths(String html) {
        Set<String> referencedTemplatePaths = new LinkedHashSet<>();
        Matcher matcher = TH_REPLACE_OR_INSERT_PATTERN.matcher(html);
        while (matcher.find()) {
            String raw = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String expression = raw.trim();
            if (expression.contains("${")) {
                continue;
            }
            if (expression.startsWith("~{") && expression.endsWith("}")) {
                expression = expression.substring(2, expression.length() - 1).trim();
            }
            int fragmentSeparator = expression.indexOf("::");
            if (fragmentSeparator <= 0) {
                continue;
            }
            String candidatePath = expression.substring(0, fragmentSeparator).trim();
            if (candidatePath.isEmpty() || candidatePath.startsWith("#") || candidatePath.startsWith("this")) {
                continue;
            }
            if (candidatePath.startsWith("'") && candidatePath.endsWith("'") && candidatePath.length() >= 2) {
                candidatePath = candidatePath.substring(1, candidatePath.length() - 1);
            } else if (candidatePath.startsWith("\"") && candidatePath.endsWith("\"") && candidatePath.length() >= 2) {
                candidatePath = candidatePath.substring(1, candidatePath.length() - 1);
            }
            if (candidatePath.startsWith("/")) {
                candidatePath = candidatePath.substring(1);
            }
            if (!candidatePath.isEmpty()) {
                referencedTemplatePaths.add(candidatePath);
            }
        }
        return referencedTemplatePaths;
    }

    private Map<String, List<String>> extractLoopVariablePaths(String html) {
        Map<String, List<String>> loopVariables = new LinkedHashMap<>();
        Matcher matcher = TH_EACH_PATTERN.matcher(html);
        while (matcher.find()) {
            String raw = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
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
            String sanitizedIterableExpression = stripStringLiterals(iterableExpression);
            List<List<String>> iterablePaths = extractModelPaths(sanitizedIterableExpression, Set.of());
            if (iterablePaths.isEmpty()) {
                continue;
            }
            List<String> iterablePath = iterablePaths.get(0);
            if (iterablePath.isEmpty()) {
                continue;
            }
            for (String alias : aliases) {
                loopVariables.putIfAbsent(alias, iterablePath);
            }
        }
        return loopVariables;
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
            if (!alias.isEmpty() && isIdentifierStart(alias.charAt(0))) {
                boolean valid = true;
                for (int i = 1; i < alias.length(); i++) {
                    if (!isIdentifierPart(alias.charAt(i))) {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    aliases.add(alias);
                }
            }
        }
        return aliases;
    }

    private Set<String> extractLocalVariablesFromThWith(String html) {
        Set<String> localVariables = new HashSet<>();
        Matcher matcher = TH_WITH_PATTERN.matcher(html);
        while (matcher.find()) {
            String raw = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            for (String assignment : splitTopLevel(raw, ',')) {
                int equalIndex = assignment.indexOf('=');
                if (equalIndex <= 0) {
                    continue;
                }
                String candidate = assignment.substring(0, equalIndex).trim();
                if (!candidate.isEmpty() && isIdentifierStart(candidate.charAt(0))) {
                    boolean valid = true;
                    for (int i = 1; i < candidate.length(); i++) {
                        if (!isIdentifierPart(candidate.charAt(i))) {
                            valid = false;
                            break;
                        }
                    }
                    if (valid) {
                        localVariables.add(candidate);
                    }
                }
            }
        }
        return localVariables;
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

    private String stripStringLiterals(String expression) {
        StringBuilder sanitized = new StringBuilder(expression.length());
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            char previous = i > 0 ? expression.charAt(i - 1) : '\0';
            if (c == '\'' && !inDoubleQuote && previous != '\\') {
                inSingleQuote = !inSingleQuote;
                sanitized.append(' ');
                continue;
            }
            if (c == '"' && !inSingleQuote && previous != '\\') {
                inDoubleQuote = !inDoubleQuote;
                sanitized.append(' ');
                continue;
            }
            sanitized.append((inSingleQuote || inDoubleQuote) ? ' ' : c);
        }
        return sanitized.toString();
    }

    private boolean isInvalidRootStartContext(String expression, int index) {
        int previousIndex = index - 1;
        while (previousIndex >= 0 && Character.isWhitespace(expression.charAt(previousIndex))) {
            previousIndex--;
        }
        if (previousIndex < 0) {
            return false;
        }
        char previous = expression.charAt(previousIndex);
        if (isIdentifierPart(previous)) {
            return true;
        }
        return previous == '#' || previous == '@' || previous == '.' || previous == '?' || previous == '\'' || previous == '"';
    }

    private boolean isFunctionCall(String expression, int identifierEnd) {
        int next = identifierEnd;
        while (next < expression.length() && Character.isWhitespace(expression.charAt(next))) {
            next++;
        }
        return next < expression.length() && expression.charAt(next) == '(';
    }

    private Object inferLeafValue(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("is")
            || normalized.startsWith("has")
            || normalized.startsWith("can")
            || normalized.startsWith("should")
            || normalized.startsWith("enabled")
            || normalized.startsWith("active")) {
            return false;
        }
        if (normalized.contains("count")
            || normalized.contains("total")
            || normalized.contains("amount")
            || normalized.contains("price")
            || normalized.contains("point")
            || normalized.contains("score")
            || normalized.contains("num")
            || normalized.contains("size")
            || normalized.contains("balance")
            || normalized.contains("age")
            || normalized.contains("rate")
            || normalized.contains("percent")) {
            return 0;
        }
        if (normalized.contains("date") || normalized.contains("time")) {
            return "2026-01-01";
        }
        if (normalized.contains("email")) {
            return "sample@example.com";
        }
        return "Sample " + key;
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }

    private String readTemplateSource(String templatePath) {
        Resource resource = resourceLoader.getResource("classpath:templates/" + templatePath + ".html");
        if (!resource.exists()) {
            return "";
        }
        try (var inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            return "";
        }
    }
}
