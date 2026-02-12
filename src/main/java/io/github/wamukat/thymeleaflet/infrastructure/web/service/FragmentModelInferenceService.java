package io.github.wamukat.thymeleaflet.infrastructure.web.service;

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
    private static final Set<String> RESERVED_ROOTS = Set.of(
        "true", "false", "null",
        "param", "session", "application", "request", "response"
    );

    private final ResourceLoader resourceLoader;

    public FragmentModelInferenceService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public Map<String, Object> inferModel(String templatePath, String fragmentName, List<String> parameterNames) {
        String html = readTemplateSource(templatePath);
        if (html.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> parameterSet = new HashSet<>(parameterNames == null ? List.of() : parameterNames);
        LinkedHashMap<String, Object> inferred = new LinkedHashMap<>();

        Matcher matcher = EXPRESSION_PATTERN.matcher(html);
        while (matcher.find()) {
            String expression = matcher.group(1);
            for (List<String> path : extractModelPaths(expression, parameterSet)) {
                putPath(inferred, path, inferLeafValue(path.get(path.size() - 1)));
            }
        }

        return inferred;
    }

    private List<List<String>> extractModelPaths(String expression, Set<String> parameterSet) {
        List<List<String>> paths = new ArrayList<>();
        int length = expression.length();
        int i = 0;
        while (i < length) {
            char current = expression.charAt(i);
            if (!isIdentifierStart(current)) {
                i++;
                continue;
            }
            if (i > 0) {
                char previous = expression.charAt(i - 1);
                if (previous == '#' || previous == '@') {
                    i++;
                    continue;
                }
            }

            int start = i;
            i++;
            while (i < length && isIdentifierPart(expression.charAt(i))) {
                i++;
            }

            String root = expression.substring(start, i);
            if (RESERVED_ROOTS.contains(root)) {
                continue;
            }
            if (parameterSet.contains(root)) {
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

    private void putPath(Map<String, Object> target, List<String> path, Object leafValue) {
        if (path.isEmpty()) {
            return;
        }
        if (path.size() == 1) {
            target.putIfAbsent(path.get(0), leafValue);
            return;
        }

        Object rootObject = target.get(path.get(0));
        Map<String, Object> rootMap;
        if (rootObject instanceof Map<?, ?> existingMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) existingMap;
            rootMap = casted;
        } else {
            rootMap = new LinkedHashMap<>();
            target.put(path.get(0), rootMap);
        }

        Map<String, Object> current = rootMap;
        for (int i = 1; i < path.size() - 1; i++) {
            String segment = path.get(i);
            Object child = current.get(segment);
            if (child instanceof Map<?, ?> childMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> casted = (Map<String, Object>) childMap;
                current = casted;
            } else {
                Map<String, Object> created = new LinkedHashMap<>();
                current.put(segment, created);
                current = created;
            }
        }
        current.putIfAbsent(path.get(path.size() - 1), leafValue);
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
