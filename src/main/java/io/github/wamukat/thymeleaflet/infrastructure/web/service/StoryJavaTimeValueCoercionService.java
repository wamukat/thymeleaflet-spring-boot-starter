package io.github.wamukat.thymeleaflet.infrastructure.web.service;

import io.github.wamukat.thymeleaflet.infrastructure.adapter.documentation.JavaDocAnalyzer;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class StoryJavaTimeValueCoercionService {

    public Map<String, Object> coerceParameters(
        Map<String, Object> parameters,
        JavaDocAnalyzer.JavaDocInfo javaDocInfo
    ) {
        Map<String, Object> coerced = deepCopyMap(parameters);
        for (JavaDocAnalyzer.ParameterInfo parameterInfo : javaDocInfo.getParameters()) {
            String name = parameterInfo.getName();
            if (coerced.containsKey(name)) {
                coerced.put(name, coerceValue(coerced.get(name), parameterInfo.getType(), name));
            }
        }
        return coerced;
    }

    public Map<String, Object> coerceModel(
        Map<String, Object> model,
        JavaDocAnalyzer.JavaDocInfo javaDocInfo
    ) {
        Map<String, Object> coerced = deepCopyMap(model);
        for (JavaDocAnalyzer.ModelInfo modelInfo : javaDocInfo.getModels()) {
            applyModelPath(coerced, modelInfo.getName(), modelInfo.getType());
        }
        return coerced;
    }

    private void applyModelPath(Map<String, Object> root, String path, String targetType) {
        if (path.isBlank()) {
            return;
        }
        String[] segments = path.split("\\.");
        applyPathSegment(root, segments, 0, path, targetType);
    }

    private @Nullable Object applyPathSegment(
        @Nullable Object current,
        String[] segments,
        int index,
        String fullPath,
        String targetType
    ) {
        if (current instanceof Map<?, ?> currentMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) currentMap;
            String segment = segments[index];
            boolean arrayWildcard = segment.endsWith("[]");
            String key = arrayWildcard ? segment.substring(0, segment.length() - 2) : segment;

            if (!map.containsKey(key)) {
                return current;
            }

            if (index == segments.length - 1) {
                if (arrayWildcard) {
                    map.put(key, coerceListElements(map.get(key), targetType, fullPath));
                } else {
                    map.put(key, coerceValue(map.get(key), targetType, fullPath));
                }
                return current;
            }

            Object next = map.get(key);
            if (arrayWildcard) {
                map.put(key, applyToListElements(next, segments, index + 1, fullPath, targetType));
            } else {
                map.put(key, applyPathSegment(next, segments, index + 1, fullPath, targetType));
            }
            return current;
        }

        if (current instanceof List<?> currentList) {
            List<Object> list = new ArrayList<>(currentList.size());
            for (Object item : currentList) {
                list.add(applyPathSegment(item, segments, index, fullPath, targetType));
            }
            return list;
        }

        return current;
    }

    private @Nullable Object applyToListElements(
        @Nullable Object value,
        String[] segments,
        int nextIndex,
        String fullPath,
        String targetType
    ) {
        if (!(value instanceof List<?> listValue)) {
            return value;
        }
        List<Object> coerced = new ArrayList<>(listValue.size());
        for (Object item : listValue) {
            coerced.add(applyPathSegment(item, segments, nextIndex, fullPath, targetType));
        }
        return coerced;
    }

    private @Nullable Object coerceListElements(@Nullable Object value, String targetType, String path) {
        if (!(value instanceof List<?> listValue)) {
            return value;
        }
        List<Object> coerced = new ArrayList<>(listValue.size());
        for (Object item : listValue) {
            coerced.add(coerceValue(item, targetType, path));
        }
        return coerced;
    }

    private @Nullable Object coerceValue(@Nullable Object value, String targetType, String path) {
        Optional<JavaTimeTarget> javaTimeTarget = JavaTimeTarget.fromTypeName(targetType);
        if (javaTimeTarget.isEmpty() || value == null || !(value instanceof String stringValue)) {
            return value;
        }
        try {
            return javaTimeTarget.orElseThrow().parse(stringValue);
        } catch (DateTimeParseException e) {
            throw new StoryValueCoercionException(
                "Failed to convert story value '%s' with value '%s' to %s"
                    .formatted(path, stringValue, targetType),
                e
            );
        }
    }

    private Map<String, Object> deepCopyMap(Map<String, Object> source) {
        Map<String, Object> copied = new HashMap<>();
        source.forEach((key, value) -> copied.put(key, deepCopyValue(value)));
        return copied;
    }

    private @Nullable Object deepCopyValue(@Nullable Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> copied = new HashMap<>();
            mapValue.forEach((key, childValue) -> copied.put(String.valueOf(key), deepCopyValue(childValue)));
            return copied;
        }
        if (value instanceof List<?> listValue) {
            List<Object> copied = new ArrayList<>(listValue.size());
            listValue.forEach(item -> copied.add(deepCopyValue(item)));
            return copied;
        }
        return value;
    }

    private enum JavaTimeTarget {
        LOCAL_DATE("LocalDate") {
            @Override
            Object parse(String value) {
                return LocalDate.parse(value);
            }
        },
        LOCAL_DATE_TIME("LocalDateTime") {
            @Override
            Object parse(String value) {
                return LocalDateTime.parse(value);
            }
        },
        LOCAL_TIME("LocalTime") {
            @Override
            Object parse(String value) {
                return LocalTime.parse(value);
            }
        },
        OFFSET_DATE_TIME("OffsetDateTime") {
            @Override
            Object parse(String value) {
                return OffsetDateTime.parse(value);
            }
        },
        ZONED_DATE_TIME("ZonedDateTime") {
            @Override
            Object parse(String value) {
                return ZonedDateTime.parse(value);
            }
        },
        INSTANT("Instant") {
            @Override
            Object parse(String value) {
                return Instant.parse(value);
            }
        };

        private final String simpleName;

        JavaTimeTarget(String simpleName) {
            this.simpleName = simpleName;
        }

        abstract Object parse(String value);

        static Optional<JavaTimeTarget> fromTypeName(String typeName) {
            String normalized = typeName.strip();
            for (JavaTimeTarget target : values()) {
                if (normalized.equals(target.simpleName) || normalized.equals("java.time." + target.simpleName)) {
                    return Optional.of(target);
                }
            }
            return Optional.empty();
        }
    }

    public static class StoryValueCoercionException extends RuntimeException {
        public StoryValueCoercionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
