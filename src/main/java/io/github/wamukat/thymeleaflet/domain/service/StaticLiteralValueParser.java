package io.github.wamukat.thymeleaflet.domain.service;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Converts safe static Thymeleaf literal snippets into Java fallback values.
 */
public final class StaticLiteralValueParser {

    private static final Pattern INTEGER_PATTERN = Pattern.compile("[-+]?\\d+");

    private StaticLiteralValueParser() {
    }

    public static Optional<Object> parse(String rawValue) {
        return parse(rawValue, "");
    }

    public static Optional<Object> parse(String rawValue, String typeName) {
        String value = rawValue.trim();
        if (value.isEmpty() || isDynamicExpression(value)) {
            return Optional.empty();
        }
        if ((value.startsWith("'") && value.endsWith("'")) || (value.startsWith("\"") && value.endsWith("\""))) {
            return Optional.of(value.substring(1, value.length() - 1));
        }
        if (value.equals("true") || value.equals("false")) {
            return Optional.of(Boolean.valueOf(value));
        }
        if (INTEGER_PATTERN.matcher(value).matches()) {
            return Optional.of(Integer.valueOf(value));
        }
        if (typeName.toLowerCase(Locale.ROOT).contains("string")) {
            return Optional.of(value);
        }
        return Optional.empty();
    }

    private static boolean isDynamicExpression(String value) {
        return value.startsWith("${")
            || value.startsWith("*{")
            || value.startsWith("@{")
            || value.startsWith("#{");
    }
}
