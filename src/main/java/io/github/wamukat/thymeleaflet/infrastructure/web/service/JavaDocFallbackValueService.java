package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.domain.service.StaticLiteralValueParser;
import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocAnalyzer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class JavaDocFallbackValueService {

    Map<String, Object> parameterDefaults(
        JavaDocAnalyzer.JavaDocInfo javaDocInfo,
        String templatePath,
        String fragmentName
    ) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (JavaDocAnalyzer.ParameterInfo parameter : javaDocInfo.getParameters()) {
            parameter.getDefaultValueOptional()
                .flatMap(value -> StaticLiteralValueParser.parse(value, parameter.getType()))
                .ifPresent(value -> values.putIfAbsent(parameter.getName(), value));
        }
        for (JavaDocAnalyzer.ExampleInfo example : javaDocInfo.getExamples()) {
            if (!matchesExample(example, templatePath, fragmentName)) {
                continue;
            }
            for (String argument : example.getArguments()) {
                namedLiteralArgument(argument).ifPresent(entry -> values.putIfAbsent(entry.name(), entry.value()));
            }
        }
        return values;
    }

    private boolean matchesExample(
        JavaDocAnalyzer.ExampleInfo example,
        String templatePath,
        String fragmentName
    ) {
        boolean templateMatches = example.getTemplatePath().isBlank() || example.getTemplatePath().equals(templatePath);
        return templateMatches && example.getFragmentName().equals(fragmentName);
    }

    Map<String, Object> modelDefaults(JavaDocAnalyzer.JavaDocInfo javaDocInfo) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (JavaDocAnalyzer.ModelInfo model : javaDocInfo.getModels()) {
            Optional<Object> defaultValue = model.getDefaultValueOptional()
                .flatMap(value -> StaticLiteralValueParser.parse(value, model.getType()));
            defaultValue.ifPresent(value -> putPath(values, List.of(model.getName().split("\\.")), value));
        }
        return values;
    }

    private Optional<NamedValue> namedLiteralArgument(String argument) {
        int assignIndex = argument.indexOf('=');
        if (assignIndex <= 0 || assignIndex >= argument.length() - 1) {
            return Optional.empty();
        }
        String name = argument.substring(0, assignIndex).trim();
        if (!isIdentifier(name)) {
            return Optional.empty();
        }
        String rawValue = argument.substring(assignIndex + 1).trim();
        return StaticLiteralValueParser.parse(rawValue).map(value -> new NamedValue(name, value));
    }

    private void putPath(Map<String, Object> root, List<String> path, Object value) {
        if (path.isEmpty()) {
            return;
        }
        Map<String, Object> current = root;
        for (int index = 0; index < path.size() - 1; index++) {
            String segment = path.get(index);
            Object child = current.get(segment);
            if (child instanceof Map<?, ?> childMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> casted = (Map<String, Object>) childMap;
                current = casted;
                continue;
            }
            Map<String, Object> created = new LinkedHashMap<>();
            current.put(segment, created);
            current = created;
        }
        current.putIfAbsent(path.getLast(), value);
    }

    private boolean isIdentifier(String name) {
        if (name.isBlank() || !Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        for (int index = 1; index < name.length(); index++) {
            if (!Character.isJavaIdentifierPart(name.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private record NamedValue(String name, Object value) {
    }
}
